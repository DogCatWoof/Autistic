package org.meow.autistic.ui.screens

import androidx.health.connect.client.HealthConnectClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.meow.autistic.data.debug.ExceptionReporter
import org.meow.autistic.data.health.HealthConnectRepository
import org.meow.autistic.data.health.HealthSnapshotEntity

@OptIn(ExperimentalCoroutinesApi::class)
class HealthConnectViewModelTest {

    private val repository = mockk<HealthConnectRepository>(relaxed = true)
    private val exceptionReporter = mockk<ExceptionReporter>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: HealthConnectViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getSdkStatus() } returns HealthConnectClient.SDK_AVAILABLE
        every { repository.requiredPermissions } returns emptySet()
        coEvery { repository.getGrantedPermissions() } returns emptySet()
        every { repository.getRecentSnapshots(any()) } returns flowOf(emptyList())
        viewModel = HealthConnectViewModel(repository, exceptionReporter)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region initial state

    @Test
    fun `sdkStatus initializes from repository`() {
        assertEquals(HealthConnectClient.SDK_AVAILABLE, viewModel.sdkStatus.value)
    }

    @Test
    fun `grantedPermissions initializes empty`() {
        assertEquals(emptySet<String>(), viewModel.grantedPermissions.value)
    }

    @Test
    fun `selectedSnapshot initializes null`() {
        assertNull(viewModel.selectedSnapshot.value)
    }

    // endregion

    // region checkPermissions

    @Test
    fun `checkPermissions updates grantedPermissions`() = runTest {
        val perms = setOf("perm1", "perm2")
        coEvery { repository.getGrantedPermissions() } returns perms
        viewModel.checkPermissions()
        assertEquals(perms, viewModel.grantedPermissions.value)
    }

    @Test
    fun `checkPermissions updates sdkStatus`() = runTest {
        every { repository.getSdkStatus() } returns HealthConnectClient.SDK_UNAVAILABLE
        viewModel.checkPermissions()
        assertEquals(HealthConnectClient.SDK_UNAVAILABLE, viewModel.sdkStatus.value)
    }

    // endregion

    // region refreshSnapshot

    @Test
    fun `refreshSnapshot calls backfillRecentDays with 7 days`() = runTest {
        viewModel.refreshSnapshot()
        coVerify { repository.backfillRecentDays(7) }
    }

    @Test
    fun `refreshSnapshot clears isRefreshing after completion`() = runTest {
        viewModel.refreshSnapshot()
        assertEquals(false, viewModel.isRefreshing.value)
    }

    // endregion

    // region selectSnapshot

    @Test
    fun `selectSnapshot stores snapshot`() {
        val snapshot = HealthSnapshotEntity(date = "2025-01-01", steps = 5000)
        viewModel.selectSnapshot(snapshot)
        assertEquals(snapshot, viewModel.selectedSnapshot.value)
    }

    @Test
    fun `selectSnapshot null clears selection`() {
        val snapshot = HealthSnapshotEntity(date = "2025-01-01")
        viewModel.selectSnapshot(snapshot)
        viewModel.selectSnapshot(null)
        assertNull(viewModel.selectedSnapshot.value)
    }

    // endregion
}
