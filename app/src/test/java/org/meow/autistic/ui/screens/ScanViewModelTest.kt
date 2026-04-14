package org.meow.autistic.ui.screens

import android.app.Application
import android.net.ConnectivityManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.product.ProductRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<ProductRepository>(relaxed = true)
    private val application = mockk<Application>(relaxed = true)
    private lateinit var viewModel: ScanViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { application.getSystemService(ConnectivityManager::class.java) } returns mockk(relaxed = true)
        viewModel = ScanViewModel(repository, application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onBarcodeDetected rejects blank barcode`() = runTest {
        viewModel.onBarcodeDetected("")
        assertEquals(0, viewModel.queue.first().size)
    }

    @Test
    fun `onBarcodeDetected rejects non-numeric barcode`() = runTest {
        viewModel.onBarcodeDetected("ABCDEFGH")
        assertEquals(0, viewModel.queue.first().size)
    }

    @Test
    fun `onBarcodeDetected rejects barcode shorter than 6 digits`() = runTest {
        viewModel.onBarcodeDetected("12345")
        assertEquals(0, viewModel.queue.first().size)
    }

    @Test
    fun `onBarcodeDetected rejects barcode longer than 14 digits`() = runTest {
        viewModel.onBarcodeDetected("123456789012345")
        assertEquals(0, viewModel.queue.first().size)
    }

    @Test
    fun `onBarcodeDetected accepts valid EAN-13`() = runTest {
        viewModel.onBarcodeDetected("5901234123457")
        assertEquals(1, viewModel.queue.first().size)
    }

    @Test
    fun `onBarcodeDetected accepts valid UPC-A`() = runTest {
        viewModel.onBarcodeDetected("012345678905")
        assertEquals(1, viewModel.queue.first().size)
    }

    @Test
    fun `onBarcodeDetected ignores duplicate barcode`() = runTest {
        viewModel.onBarcodeDetected("5901234123457")
        viewModel.onBarcodeDetected("5901234123457")
        assertEquals(1, viewModel.queue.first().size)
    }
}
