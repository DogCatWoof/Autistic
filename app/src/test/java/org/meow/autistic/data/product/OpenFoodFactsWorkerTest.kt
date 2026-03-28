package org.meow.autistic.data.product

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsWorkerTest {

    private val baseUrl = "https://static.openfoodfacts.org/data/delta/"

    // index.txt listing two delta files with timestamps in seconds
    private val sampleIndex = """
        openfoodfacts_products_1000000000_1000086400.json.gz
        openfoodfacts_products_1000086400_1000172800.json.gz
        openfoodfacts_products_1000172800_1000259200.json.gz
    """.trimIndent()

    @Test
    fun `parseDeltaUrls returns only files newer than lastSync`() {
        // lastSync in millis: 1000086400 seconds → 1000086400000 ms
        // Only the last two files have endTimestamp > 1000086400
        val lastSyncMs = 1000086400L * 1000L
        val result = OpenFoodFactsWorker.parseDeltaUrls(sampleIndex, lastSyncMs)

        assertEquals(2, result.size)
        assertTrue(result[0].contains("1000086400_1000172800"))
        assertTrue(result[1].contains("1000172800_1000259200"))
    }

    @Test
    fun `parseDeltaUrls returns all files when lastSync is before all entries`() {
        val lastSyncMs = 999999999L * 1000L
        val result = OpenFoodFactsWorker.parseDeltaUrls(sampleIndex, lastSyncMs)
        assertEquals(3, result.size)
    }

    @Test
    fun `parseDeltaUrls returns empty when lastSync is after all entries`() {
        val lastSyncMs = 2000000000L * 1000L
        val result = OpenFoodFactsWorker.parseDeltaUrls(sampleIndex, lastSyncMs)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseDeltaUrls returns results in ascending order`() {
        val shuffled = """
            openfoodfacts_products_1000172800_1000259200.json.gz
            openfoodfacts_products_1000000000_1000086400.json.gz
            openfoodfacts_products_1000086400_1000172800.json.gz
        """.trimIndent()
        val result = OpenFoodFactsWorker.parseDeltaUrls(shuffled, 0L)
        assertEquals(
            listOf(
                "${baseUrl}openfoodfacts_products_1000000000_1000086400.json.gz",
                "${baseUrl}openfoodfacts_products_1000086400_1000172800.json.gz",
                "${baseUrl}openfoodfacts_products_1000172800_1000259200.json.gz",
            ),
            result
        )
    }

    @Test
    fun `parseDeltaUrls skips blank and malformed lines`() {
        val messyIndex = """

            not-a-valid-filename.gz
            openfoodfacts_products_1000000000_1000086400.json.gz
            also-invalid
        """.trimIndent()
        val result = OpenFoodFactsWorker.parseDeltaUrls(messyIndex, 0L)
        assertEquals(1, result.size)
        assertTrue(result[0].contains("1000000000_1000086400"))
    }

    @Test
    fun `parseDeltaUrls returns empty for empty index`() {
        val result = OpenFoodFactsWorker.parseDeltaUrls("", 0L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseDeltaUrls constructs correct full URL`() {
        val index = "openfoodfacts_products_1000000000_1000086400.json.gz"
        val result = OpenFoodFactsWorker.parseDeltaUrls(index, 0L)
        assertEquals("${baseUrl}openfoodfacts_products_1000000000_1000086400.json.gz", result[0])
    }
}
