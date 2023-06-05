package de.seemoo.at_tracking_detection.ui.scan

import android.animation.ObjectAnimator
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.animation.addListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import de.seemoo.at_tracking_detection.R
import de.seemoo.at_tracking_detection.database.models.device.BaseDevice.Companion.getBatteryState
import de.seemoo.at_tracking_detection.database.models.device.BaseDevice.Companion.getBatteryStateAsString
import de.seemoo.at_tracking_detection.database.models.device.BaseDevice.Companion.getConnectionStateAsString
import de.seemoo.at_tracking_detection.util.ble.BLEScanner
import de.seemoo.at_tracking_detection.database.models.device.BaseDevice.Companion.getPublicKey
import de.seemoo.at_tracking_detection.database.models.device.BatteryState
import de.seemoo.at_tracking_detection.databinding.FragmentScanDistanceBinding
import de.seemoo.at_tracking_detection.util.Utility
import timber.log.Timber

class ScanDistanceFragment : Fragment() {
    private val viewModel: ScanDistanceViewModel by viewModels()
    private val safeArgs: ScanDistanceFragmentArgs by navArgs()

    private var deviceAddress: String? = null

    private var oldAnimationValue = 0f
    private val animationDuration = 1000L

    private lateinit var binding: FragmentScanDistanceBinding

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val publicKey = safeArgs.deviceAddress

                if (publicKey == null) {
                    showSearchMessage()
                }

                if (getPublicKey(it) == publicKey){
                    viewModel.bluetoothRssi.postValue(it.rssi)
                    val connectionState = getConnectionStateAsString(it)
                    viewModel.connectionState.postValue(connectionState)
                    val batteryState = getBatteryState(it)
                    val batteryStateString = getBatteryStateAsString(it)
                    viewModel.batteryState.postValue(batteryStateString)
                    val connectionQuality = Utility.dbmToPercent(it.rssi).toFloat()
                    val displayedConnectionQuality = (connectionQuality * 100).toInt()
                    viewModel.connectionQuality.postValue(displayedConnectionQuality)

                    println(viewModel.isFirstScanCallback.value!!)

                    setBattery(batteryState)
                    setHeight(connectionQuality)

                    if (viewModel.isFirstScanCallback.value!!) {
                        viewModel.isFirstScanCallback.value = false
                        removeSearchMessage()
                    }
                }

            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Timber.e("BLE Scan failed. $errorCode")
            stopBluetoothScan()
            view?.let {
                Snackbar.make(
                    it,
                    R.string.ble_service_connection_error,
                    Snackbar.LENGTH_LONG
                )
            }
        }
    }

    private fun removeSearchMessage() {
        binding.scanResultLoadingBar.visibility = View.GONE
        binding.searchingForDevice.visibility = View.GONE
        binding.connectionQuality.visibility = View.VISIBLE
        binding.batteryLayout.visibility = View.VISIBLE
    }

    private fun showSearchMessage() {
        binding.scanResultLoadingBar.visibility = View.VISIBLE
        binding.searchingForDevice.visibility = View.VISIBLE
        binding.connectionQuality.visibility = View.GONE
        binding.batteryLayout.visibility = View.GONE
    }

    private fun setHeight(connectionQuality: Float, speed: Long = animationDuration) {
        val viewHeight = binding.backgroundBar.height
        val targetHeight: Float = connectionQuality * viewHeight * (-1) + viewHeight

        ObjectAnimator.ofFloat(
            binding.backgroundBar,
            "translationY",
            oldAnimationValue,
            targetHeight
        ).apply {
            cancel() // cancels any old animation
            duration = speed
            addListener(onEnd = {
                // only changes the value after the animation is done
                oldAnimationValue = targetHeight
            })
            start()
        }
    }

    private fun setBattery(batteryState: BatteryState) {
        when(batteryState) {
            BatteryState.FULL -> binding.batterySymbol.setImageDrawable(resources.getDrawable(R.drawable.ic_battery_full_24))
            BatteryState.MEDIUM -> binding.batterySymbol.setImageDrawable(resources.getDrawable(R.drawable.ic_battery_medium_24))
            BatteryState.LOW -> binding.batterySymbol.setImageDrawable(resources.getDrawable(R.drawable.ic_battery_low_24))
            BatteryState.VERY_LOW -> binding.batterySymbol.setImageDrawable(resources.getDrawable(R.drawable.ic_battery_very_low_24))
            else -> binding.batterySymbol.setImageDrawable(resources.getDrawable(R.drawable.ic_battery_unknown_24))
        }
    }

    private fun startBluetoothScan() {
        // Start a scan if the BLEScanner is not already running
        if (!BLEScanner.isScanning) {
            BLEScanner.startBluetoothScan(this.requireContext())
        }

        // Register the current fragment as a callback
        BLEScanner.registerCallback(this.scanCallback)
    }

    private fun stopBluetoothScan() {
        // We just unregister the callback, but keep the scanner running
        // until the app is closed / moved to background
        BLEScanner.unregisterCallback(this.scanCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_scan_distance,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = viewModel

        // This is called deviceAddress but contains the ID
        deviceAddress = safeArgs.deviceAddress
        viewModel.deviceAddress.postValue(deviceAddress)

        viewModel.isFirstScanCallback.postValue(true)
        showSearchMessage()

        startBluetoothScan()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.isFirstScanCallback.postValue(true)
        showSearchMessage()
        startBluetoothScan()
    }

    override fun onPause() {
        super.onPause()
        showSearchMessage()
        stopBluetoothScan()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopBluetoothScan()
    }

}