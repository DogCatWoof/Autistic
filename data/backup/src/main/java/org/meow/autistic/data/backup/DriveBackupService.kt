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
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.crypto.AEADBadTagException

private const val APP_NAME = "Autistic"
private const val DB_NAME = "autistic_database"
private const val FOLDER_NAME = "Autism Backups"
private const val FOLDER_MIME = "application/vnd.google-apps.folder"
private const val BACKUP_PREFIX = "autistic_db_"
private const val BACKUP_SUFFIX = ".enc"
private const val MAX_BACKUPS = 7

/** Legacy unencrypted backup — used only as a restore fallback. */
private const val LEGACY_BACKUP_NAME = "autistic_db_backup.sqlite"

private const val ENCRYPTED_MIME = "application/octet-stream"
private const val TAG = "DriveBackupService"

private val BACKUP_TIMESTAMP_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

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
 * Each backup is a new timestamped AES-256-GCM encrypted file stored inside
 * the "Autism Backups" folder in the user's My Drive. The [MAX_BACKUPS] most
 * recent backups are retained; older ones are pruned after each successful backup.
 * Legacy unencrypted root-level backups are supported as a restore fallback.
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
     * Checkpoints WAL, encrypts the database, and uploads it as a new timestamped
     * file inside the "Autism Backups" Drive folder. Prunes backups beyond [MAX_BACKUPS].
     * Logs and swallows errors so a backup failure never blocks the daily reset.
     */
    suspend fun backupDatabase() {
        try {
            backupDatabaseInternal()
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed — proceeding anyway", e)
        }
    }

    /** Returns true if any encrypted (or legacy) backup exists on Drive. */
    suspend fun hasRemoteBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val drive = buildDrive()
            val folderId = findFolderId(drive)
            if (folderId != null && findLatestBackup(drive, folderId) != null) return@withContext true
            findRootFileId(drive, LEGACY_BACKUP_NAME) != null
        } catch (e: Exception) {
            Log.w(TAG, "Could not check for remote backup", e)
            false
        }
    }

    /**
     * Downloads the most recent backup from Drive, decrypts it, replaces the local
     * database file, and closes the Room singleton so the caller can restart the process.
     *
     * The caller must restart the process after [RestoreResult.Success] so Room
     * reopens against the restored database.
     */
    suspend fun restoreDatabase(): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val drive = buildDrive()
            val (fileId, encrypted) = resolveRestoreFile(drive)
                ?: return@withContext RestoreResult.NotFound

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

        TaskDatabase.getDatabase(context).checkpoint()

        val plainBytes = dbFile.readBytes()
        val encryptedBytes = encryptor.encrypt(plainBytes)

        val drive = buildDrive()
        val folderId = getOrCreateBackupFolder(drive)
        val fileName = "$BACKUP_PREFIX${BACKUP_TIMESTAMP_FMT.format(Instant.now())}$BACKUP_SUFFIX"
        val content = ByteArrayContent(ENCRYPTED_MIME, encryptedBytes)

        val created = drive.files()
            .create(
                File().apply {
                    name = fileName
                    parents = listOf(folderId)
                },
                content,
            )
            .setFields("id")
            .execute()
        Log.i(TAG, "Backup created: $fileName (id=${created.id})")

        pruneOldBackups(drive, folderId)

        context.driveBackupDataStore.edit { it[LAST_BACKUP_KEY] = System.currentTimeMillis() }
    }

    /** Finds or creates the "Autism Backups" folder in My Drive root; returns its ID. */
    private fun getOrCreateBackupFolder(drive: Drive): String {
        findFolderId(drive)?.let { return it }
        val folder = drive.files()
            .create(
                File().apply {
                    name = FOLDER_NAME
                    mimeType = FOLDER_MIME
                },
            )
            .setFields("id")
            .execute()
        Log.i(TAG, "Created Drive folder '$FOLDER_NAME' (id=${folder.id})")
        return folder.id
    }

    /** Returns the Drive ID of the most recent backup file in [folderId], or null. */
    private fun findLatestBackup(drive: Drive, folderId: String): String? =
        drive.files().list()
            .setSpaces("drive")
            .setFields("files(id,name,createdTime)")
            .setQ("'$folderId' in parents and name contains '$BACKUP_PREFIX' and trashed = false")
            .setOrderBy("createdTime desc")
            .setPageSize(1)
            .execute()
            .files
            ?.firstOrNull()
            ?.id

    /** Deletes backups in [folderId] beyond the [MAX_BACKUPS] most recent. */
    private fun pruneOldBackups(drive: Drive, folderId: String) {
        val files = drive.files().list()
            .setSpaces("drive")
            .setFields("files(id,name,createdTime)")
            .setQ("'$folderId' in parents and name contains '$BACKUP_PREFIX' and trashed = false")
            .setOrderBy("createdTime desc")
            .execute()
            .files ?: return

        files.drop(MAX_BACKUPS).forEach { file ->
            drive.files().delete(file.id).execute()
            Log.i(TAG, "Pruned old backup: ${file.name}")
        }
    }

    /**
     * Resolves the best available restore source as (fileId, isEncrypted).
     * Prefers the most recent file in the "Autism Backups" folder; falls back
     * to a legacy unencrypted backup at Drive root.
     */
    private fun resolveRestoreFile(drive: Drive): Pair<String, Boolean>? {
        findFolderId(drive)?.let { folderId ->
            findLatestBackup(drive, folderId)?.let { return it to true }
        }
        findRootFileId(drive, LEGACY_BACKUP_NAME)?.let { return it to false }
        return null
    }

    /** Looks up the "Autism Backups" folder without creating it; returns id or null. */
    private fun findFolderId(drive: Drive): String? =
        drive.files().list()
            .setSpaces("drive")
            .setFields("files(id)")
            .setQ("mimeType = '$FOLDER_MIME' and name = '$FOLDER_NAME' and 'root' in parents and trashed = false")
            .execute()
            .files
            ?.firstOrNull()
            ?.id

    /** Finds a file by exact name anywhere in Drive (used for legacy backup lookup). */
    private fun findRootFileId(drive: Drive, name: String): String? =
        drive.files().list()
            .setSpaces("drive")
            .setFields("files(id)")
            .setQ("name = '$name' and trashed = false")
            .execute()
            .files
            ?.firstOrNull()
            ?.id

    private fun writeRestoredDatabase(dbBytes: ByteArray) {
        val dbFile = context.getDatabasePath(DB_NAME)
        TaskDatabase.closeInstance()
        dbFile.parentFile?.mkdirs()
        dbFile.writeBytes(dbBytes)
        context.getDatabasePath("$DB_NAME-wal").delete()
        context.getDatabasePath("$DB_NAME-shm").delete()
        Log.i(TAG, "Database restored (${dbBytes.size} bytes)")
    }

    private fun downloadFile(drive: Drive, fileId: String): ByteArray {
        val baos = ByteArrayOutputStream()
        drive.files().get(fileId).setAlt("media").executeMediaAndDownloadTo(baos)
        return baos.toByteArray()
    }

    private suspend fun buildDrive(): Drive {
        val token = tokenProvider()
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpRequestInitializer { it.headers.authorization = "Bearer $token" },
        ).setApplicationName(APP_NAME).build()
    }
}
