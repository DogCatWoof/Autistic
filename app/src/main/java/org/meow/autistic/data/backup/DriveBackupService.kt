package org.meow.autistic.data.backup

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.meow.autistic.data.task.TaskDatabase
import java.io.ByteArrayOutputStream
import javax.crypto.AEADBadTagException

private const val APP_NAME = "Autistic"
private const val DB_NAME = "autistic_database"

/** Encrypted backup (current format). */
private const val ENCRYPTED_BACKUP_NAME = "autistic_db_backup.enc"

/** Legacy unencrypted backup — used only for restore fallback. */
private const val LEGACY_BACKUP_NAME = "autistic_db_backup.sqlite"

private const val ENCRYPTED_MIME = "application/octet-stream"
private const val TAG = "DriveBackupService"

private val Context.driveBackupDataStore: DataStore<Preferences>
    by preferencesDataStore("drive_backup_prefs")

private val LAST_BACKUP_KEY = longPreferencesKey("last_backup_ms")

/** Result of a restore attempt. */
sealed class RestoreResult {
    object Success : RestoreResult()
    object NotFound : RestoreResult()
    object DecryptionFailed : RestoreResult()
    data class Error(val message: String) : RestoreResult()
}

/**
 * Backs up and restores the Room database via Google Drive.
 *
 * Backups are AES-256-GCM encrypted (Android Keystore key) and stored as
 * [ENCRYPTED_BACKUP_NAME] in the user's Drive. Only the most recent backup is kept.
 * Legacy unencrypted backups are supported as a restore fallback.
 */
class DriveBackupService(
    private val context: Context,
    private val tokenProvider: suspend () -> String,
) {
    private val encryptor by lazy { BackupEncryptor.create() }

    companion object {
        /** Emits the ms-epoch timestamp of the last successful backup, or null. */
        fun getLastBackupFlow(context: Context): Flow<Long?> =
            context.driveBackupDataStore.data.map { it[LAST_BACKUP_KEY] }
    }

    /**
     * Checkpoints WAL, encrypts, and uploads the database.
     * Logs and swallows errors so a backup failure never blocks the daily reset.
     */
    suspend fun backupDatabase() {
        try {
            backupDatabaseInternal()
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed — proceeding anyway", e)
        }
    }

    /** Returns true if an encrypted (or legacy) backup exists on Drive. */
    suspend fun hasRemoteBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = buildDrive()
            findFileId(drive, ENCRYPTED_BACKUP_NAME) != null
                || findFileId(drive, LEGACY_BACKUP_NAME) != null
        } catch (e: Exception) {
            Log.w(TAG, "Could not check for remote backup", e)
            false
        }
    }

    /**
     * Downloads the backup from Drive, decrypts it, replaces the local database file,
     * and closes the Room singleton so the caller can restart the process.
     *
     * The caller must restart the process after [RestoreResult.Success] so Room
     * reopens against the restored database.
     */
    suspend fun restoreDatabase(): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val drive = buildDrive()

            val (fileId, encrypted) = when {
                findFileId(drive, ENCRYPTED_BACKUP_NAME) != null -> {
                    val id = findFileId(drive, ENCRYPTED_BACKUP_NAME)!!
                    id to true
                }
                findFileId(drive, LEGACY_BACKUP_NAME) != null -> {
                    val id = findFileId(drive, LEGACY_BACKUP_NAME)!!
                    id to false
                }
                else -> return@withContext RestoreResult.NotFound
            }

            val rawBytes = downloadFile(drive, fileId)

            val dbBytes = if (encrypted) {
                try {
                    encryptor.decrypt(rawBytes)
                } catch (e: AEADBadTagException) {
                    Log.e(TAG, "Decryption failed — backup may be from a different device", e)
                    return@withContext RestoreResult.DecryptionFailed
                }
            } else {
                rawBytes
            }

            writeRestoredDatabase(dbBytes)
            RestoreResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            RestoreResult.Error(e.message ?: "Unknown error")
        }
    }

    // ── private ──────────────────────────────────────────────────────────────

    private suspend fun backupDatabaseInternal() = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            Log.w(TAG, "Database file not found at ${dbFile.absolutePath}, skipping")
            return@withContext
        }

        // Flush WAL to main file before reading.
        TaskDatabase.getDatabase(context).checkpoint()

        val plainBytes = dbFile.readBytes()
        val encryptedBytes = encryptor.encrypt(plainBytes)

        val drive = buildDrive()
        val content = ByteArrayContent(ENCRYPTED_MIME, encryptedBytes)
        val existingId = findFileId(drive, ENCRYPTED_BACKUP_NAME)

        if (existingId != null) {
            drive.files().update(existingId, File(), content).execute()
            Log.i(TAG, "Encrypted backup updated (id=$existingId)")
        } else {
            val created = drive.files()
                .create(File().apply { name = ENCRYPTED_BACKUP_NAME }, content)
                .setFields("id")
                .execute()
            Log.i(TAG, "Encrypted backup created (id=${created.id})")
        }

        context.driveBackupDataStore.edit { it[LAST_BACKUP_KEY] = System.currentTimeMillis() }
    }

    private fun writeRestoredDatabase(dbBytes: ByteArray) {
        val dbFile = context.getDatabasePath(DB_NAME)

        // Close Room so it releases file handles before we overwrite.
        TaskDatabase.closeInstance()

        dbFile.parentFile?.mkdirs()
        dbFile.writeBytes(dbBytes)

        // Remove stale WAL/SHM files so Room doesn't apply old journal entries.
        context.getDatabasePath("$DB_NAME-wal").delete()
        context.getDatabasePath("$DB_NAME-shm").delete()

        Log.i(TAG, "Database restored (${dbBytes.size} bytes)")
    }

    private fun downloadFile(drive: Drive, fileId: String): ByteArray {
        val baos = ByteArrayOutputStream()
        drive.files().get(fileId).setAlt("media").executeMediaAndDownloadTo(baos)
        return baos.toByteArray()
    }

    private fun findFileId(drive: Drive, name: String): String? =
        drive.files().list()
            .setSpaces("drive")
            .setFields("files(id,name)")
            .setQ("name = '$name' and trashed = false")
            .execute()
            .files
            ?.firstOrNull()
            ?.id

    private suspend fun buildDrive(): Drive {
        val token = tokenProvider()
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpRequestInitializer { it.headers.authorization = "Bearer $token" },
        ).setApplicationName(APP_NAME).build()
    }
}
