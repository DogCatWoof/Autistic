package org.meow.autistic.data.todo

import android.os.SystemClock
import kotlinx.coroutines.flow.Flow
import org.meow.autistic.data.diagnostics.QueryLogger

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
        timed("DailyTaskRepository.insert") { dao.insert(task) }

    suspend fun update(task: DailyTaskEntity) =
        timed("DailyTaskRepository.update") { dao.update(task) }

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
