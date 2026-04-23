package org.meow.autistic.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Reads Health Connect data and caches it in [HealthSnapshotDao]. */
class HealthConnectRepository(
    private val context: Context,
    private val dao: HealthSnapshotDao,
) {
    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class),
    )

    fun getSdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    fun getTodaySnapshot(): Flow<HealthSnapshotEntity?> =
        dao.getByDate(todayKey())

    suspend fun getGrantedPermissions(): Set<String> {
        val client = clientOrNull() ?: return emptySet()
        return client.permissionController.getGrantedPermissions()
    }

    suspend fun refreshTodaySnapshot(): HealthSnapshotEntity? {
        val client = clientOrNull() ?: return null
        val date = todayKey()
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = Instant.now()

        val steps = readTodaySteps(client, start, end)
        val sleep = readLastNightSleep(client)
        val heartRate = readRecentAvgHeartRate(client, start, end)
        val weight = readLatestWeight(client)
        val calories = readTodayCaloriesBurned(client, start, end)
        val glucose = readLatestBloodGlucose(client, start, end)

        val snapshot = HealthSnapshotEntity(
            date = date,
            steps = steps,
            sleepMinutes = sleep,
            avgHeartRateBpm = heartRate,
            weightKg = weight,
            caloriesBurned = calories,
            bloodGlucoseMmol = glucose,
            lastUpdatedAt = Instant.now(),
        )
        dao.upsert(snapshot)
        return snapshot
    }

    private suspend fun readTodaySteps(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Long? = runCatching {
        val response = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))
        )
        response.records.sumOf { it.count }.takeIf { it > 0 }
    }.getOrNull()

    private suspend fun readLastNightSleep(client: HealthConnectClient): Long? = runCatching {
        val end = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val start = end.minusSeconds(12 * 3600)
        val response = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start, end))
        )
        val totalMs = response.records.sumOf {
            it.endTime.toEpochMilli() - it.startTime.toEpochMilli()
        }
        (totalMs / 60_000).takeIf { it > 0 }
    }.getOrNull()

    private suspend fun readRecentAvgHeartRate(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Double? = runCatching {
        val response = client.readRecords(
            ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(start, end))
        )
        val samples = response.records.flatMap { it.samples }.map { it.beatsPerMinute }
        if (samples.isEmpty()) null else samples.average()
    }.getOrNull()

    private suspend fun readLatestWeight(client: HealthConnectClient): Double? = runCatching {
        val end = Instant.now()
        val start = end.minusSeconds(30L * 24 * 3600)
        val response = client.readRecords(
            ReadRecordsRequest(
                WeightRecord::class,
                TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1,
            )
        )
        response.records.firstOrNull()?.weight?.inKilograms
    }.getOrNull()

    private suspend fun readTodayCaloriesBurned(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Double? = runCatching {
        val response = client.readRecords(
            ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))
        )
        val total = response.records.sumOf { it.energy.inKilocalories }
        total.takeIf { it > 0.0 }
    }.getOrNull()

    private suspend fun readLatestBloodGlucose(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
    ): Double? = runCatching {
        val response = client.readRecords(
            ReadRecordsRequest(BloodGlucoseRecord::class, TimeRangeFilter.between(start, end))
        )
        response.records.lastOrNull()?.level?.inMillimolesPerLiter
    }.getOrNull()

    private fun clientOrNull(): HealthConnectClient? = runCatching {
        if (getSdkStatus() == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }.getOrNull()

    private fun todayKey(): String =
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
}
