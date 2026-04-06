package org.meow.autistic.data.diagnostics

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryLoggerTest {

    private val logger = QueryLogger()

    @Test
    fun `log adds an entry when duration exceeds threshold`() = runTest {
        logger.log("Repo.method", 10_000L)
        assertEquals(1, logger.entries.value.size)
    }

    @Test
    fun `log ignores entry below threshold`() = runTest {
        logger.log("Repo.method", 9_999L)
        assertTrue(logger.entries.value.isEmpty())
    }

    @Test
    fun `entries are in newest-first order`() = runTest {
        logger.log("first", 10_000L)
        logger.log("second", 10_001L)
        assertEquals("second", logger.entries.value[0].label)
        assertEquals("first", logger.entries.value[1].label)
    }

    @Test
    fun `entries are capped at 200`() = runTest {
        repeat(250) { i -> logger.log("label$i", 10_000L + i.toLong()) }
        assertEquals(200, logger.entries.value.size)
    }

    @Test
    fun `cap keeps the 200 most recent entries`() = runTest {
        repeat(250) { i -> logger.log("label$i", 10_000L + i.toLong()) }
        assertEquals("label249", logger.entries.value.first().label)
        assertEquals("label50", logger.entries.value.last().label)
    }

    @Test
    fun `clear removes all entries`() = runTest {
        logger.log("Repo.method", 10_000L)
        logger.clear()
        assertTrue(logger.entries.value.isEmpty())
    }

    @Test
    fun `entry stores correct label and durationMs`() = runTest {
        logger.log("TodoRepository.insert", 15_000L)
        val entry = logger.entries.value.first()
        assertEquals("TodoRepository.insert", entry.label)
        assertEquals(15_000L, entry.durationMs)
    }

    @Test
    fun `entry timestamp is non-zero`() = runTest {
        logger.log("Repo.method", 10_000L)
        assertTrue(logger.entries.value.first().timestamp > 0L)
    }
}
