package de.seemoo.at_tracking_detection.database.repository

import androidx.annotation.WorkerThread
import de.seemoo.at_tracking_detection.database.daos.DeviceDao
import de.seemoo.at_tracking_detection.database.relations.DeviceBeaconNotification
import de.seemoo.at_tracking_detection.database.models.device.BaseDevice
import de.seemoo.at_tracking_detection.database.models.device.DeviceType
import de.seemoo.at_tracking_detection.util.risk.RiskLevel
import de.seemoo.at_tracking_detection.util.risk.RiskLevelEvaluator
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject

@WorkerThread
class DeviceRepository @Inject constructor(private val deviceDao: DeviceDao) {
    val devices: Flow<List<BaseDevice>> = deviceDao.getAll()

    fun trackingDevicesSince(since: LocalDateTime) = deviceDao.getAllNotificationSince(since)

    fun trackingDevicesNotIgnoredSince(since: LocalDateTime) = deviceDao.getAllTrackingDevicesNotIgnoredSince(since)

    fun trackingDevicesNotIgnoredSinceCount(since: LocalDateTime) = deviceDao.getAllTrackingDevicesNotIgnoredSinceCount(since)

    fun getCachedRiskLevel(deviceAddress: String): Int = deviceDao.getCachedRiskLevel(deviceAddress)

    fun getLastCachedRiskLevelDate(deviceAddress: String): LocalDateTime? = deviceDao.getLastCachedRiskLevelDate(deviceAddress)

    fun updateRiskLevelCache(deviceAddress: String, riskLevel: Int, lastCalculatedRiskDate: LocalDateTime)
            = deviceDao.updateRiskLevelCache(deviceAddress, riskLevel, lastCalculatedRiskDate)

    fun trackingDevicesSinceCount(since: LocalDateTime) = deviceDao.trackingDevicesCount(since)

    val totalCount: Flow<Int> = deviceDao.getTotalCount()

    fun totalDeviceCountChange(since: LocalDateTime): Flow<Int> =
        deviceDao.getTotalCountChange(since)

    fun devicesCurrentlyMonitored(since: LocalDateTime): Flow<Int> =
        deviceDao.getCurrentlyMonitored(since)

    fun deviceCountSince(since: LocalDateTime): Flow<Int> =
        deviceDao.getCurrentlyMonitored(since)

    val ignoredDevices: Flow<List<BaseDevice>> = deviceDao.getIgnored()

    val ignoredDevicesSync: List<BaseDevice> = deviceDao.getIgnoredSync()

    fun getDevice(deviceAddress: String): BaseDevice? = deviceDao.getByAddress(deviceAddress)

    val countNotTracking = deviceDao.getCountNotTracking(RiskLevelEvaluator.relevantTrackingDate)

    val countIgnored = deviceDao.getCountIgnored()

    fun countForDeviceType(deviceType: DeviceType) = deviceDao.getCountForType(deviceType.name, RiskLevelEvaluator.relevantTrackingDate)
    fun countForDeviceTypes(deviceType1: DeviceType, deviceType2: DeviceType) = deviceDao.getCountForTypes(deviceType1.name, deviceType2.name, RiskLevelEvaluator.relevantTrackingDate)

    fun getNumberOfLocationsForDeviceSince(deviceAddress: String, since: LocalDateTime): Int = deviceDao.getNumberOfLocationsForDevice(deviceAddress, since)

    fun getNumberOfLocationsForDeviceWithAccuracyLimitSince(deviceAddress: String, maxAccuracy: Float, since: LocalDateTime): Int = deviceDao.getNumberOfLocationsForWithAccuracyLimitDevice(deviceAddress, maxAccuracy, since)

    @WorkerThread
    suspend fun getDeviceBeaconsSince(dateTime: String?): List<DeviceBeaconNotification> {
        return if (dateTime != null) {
            deviceDao.getDeviceBeaconsSince(LocalDateTime.parse(dateTime))
        } else {
            deviceDao.getDeviceBeacons()
        }
    }

    suspend fun getDeviceBeaconsSinceDate(dateTime: LocalDateTime?): List<DeviceBeaconNotification> {
        return if (dateTime != null) {
            deviceDao.getDeviceBeaconsSince(dateTime)
        } else {
            deviceDao.getDeviceBeacons()
        }
    }

    @WorkerThread
    suspend fun insert(baseDevice: BaseDevice) {
        deviceDao.insert(baseDevice)
    }

    @WorkerThread
    suspend fun update(baseDevice: BaseDevice) {
        deviceDao.update(baseDevice)
    }

    @WorkerThread
    suspend fun setIgnoreFlag(deviceAddress: String, state: Boolean) {
        deviceDao.setIgnoreFlag(deviceAddress, state)
    }
}