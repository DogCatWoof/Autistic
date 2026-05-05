package org.meow.autistic.data.task

import android.os.SystemClock
import kotlinx.coroutines.flow.Flow
import org.meow.autistic.data.diagnostics.QueryLogger
import java.time.Instant

/**
 * Persistence layer for [DailyTaskEntity].
 * Delegates to [DailyTaskDao] and records timing via [QueryLogger].
 */
class DailyTaskRepository(
    private val dao: DailyTaskDao,
    private val queryLogger: QueryLogger,
) {
    val allTasks: Flow<List<DailyTaskEntity>> = dao.getAll()

    suspend fun getAllOnce(): List<DailyTaskEntity> =
        timed("DailyTaskRepository.getAllOnce") { dao.getAllOnce() }

    suspend fun insert(task: DailyTaskEntity): Long =
        timed("DailyTaskRepository.insert") {
            dao.insert(task.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
        }

    suspend fun update(task: DailyTaskEntity) =
        timed("DailyTaskRepository.update") {
            dao.update(task.copy(lastModifiedAt = Instant.now(), pendingFirestoreSync = true))
        }

    suspend fun delete(task: DailyTaskEntity) =
        timed("DailyTaskRepository.delete") { dao.delete(task) }

    private suspend inline fun <T> timed(label: String, block: suspend () -> T): T {
        val start = SystemClock.elapsedRealtime()
        return try {
            block()
        } finally {
            queryLogger.log(label, SystemClock.elapsedRealtime() - start)
        }
    }
}
