package org.meow.autistic.data.backup

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val APP_NAME = "Autistic"
private const val BACKUP_FILE_NAME = "autistic_db_backup.sqlite"
private const val DB_MIME_TYPE = "application/octet-stream"
private const val TAG = "DriveBackupService"

private val Context.driveBackupDataStore: DataStore<Preferences>
    by preferencesDataStore("drive_backup_prefs")

private val LAST_BACKUP_KEY = longPreferencesKey("last_backup_ms")

/**
 * Backs up the Room database file to the user's Google Drive (My Drive root).
 *
 * The file is visible in drive.google.com under the name [BACKUP_FILE_NAME].
 * Only the latest backup is kept — previous backups are overwritten.
 *
 * @param context Used to locate the database file path.
 * @param tokenProvider Suspending supplier of a valid OAuth access token.
 */
class DriveBackupService(
    private val context: Context,
    private val tokenProvider: suspend () -> String,
) {
    companion object {
        /** Emits the timestamp (ms since epoch) of the last successful backup, or null. */
        fun getLastBackupFlow(context: Context): Flow<Long?> =
            context.driveBackupDataStore.data.map { it[LAST_BACKUP_KEY] }
    }

    /**
     * Uploads the current state of `autistic_database` to Google Drive.
     * Logs and swallows errors so a backup failure never blocks the daily reset.
     */
    suspend fun backupDatabase() {
        try {
            backupDatabaseInternal()
        } catch (e: Exception) {
            Log.e(TAG, "DB backup failed — daily reset will proceed anyway", e)
        }
    }

    private suspend fun backupDatabaseInternal() = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath("autistic_database")
        if (!dbFile.exists()) {
            Log.w(TAG, "Database file not found at ${dbFile.absolutePath}, skipping backup")
            return@withContext
        }

        val token = tokenProvider()
        val drive = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            HttpRequestInitializer { request ->
                request.headers.authorization = "Bearer $token"
            },
        ).setApplicationName(APP_NAME).build()

        val existingId = findExistingBackupId(drive)
        val content = FileContent(DB_MIME_TYPE, dbFile)

        if (existingId != null) {
            drive.files()
                .update(existingId, File(), content)
                .execute()
            Log.i(TAG, "Database backup updated (Drive file id=$existingId)")
        } else {
            val metadata = File().apply {
                name = BACKUP_FILE_NAME
            }
            val created = drive.files()
                .create(metadata, content)
                .setFields("id")
                .execute()
            Log.i(TAG, "Database backup created (Drive file id=${created.id})")
        }
        context.driveBackupDataStore.edit { it[LAST_BACKUP_KEY] = System.currentTimeMillis() }
    }

    private fun findExistingBackupId(drive: Drive): String? {
        val result = drive.files().list()
            .setSpaces("drive")
            .setFields("files(id,name)")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .execute()
        return result.files?.firstOrNull()?.id
    }
}
