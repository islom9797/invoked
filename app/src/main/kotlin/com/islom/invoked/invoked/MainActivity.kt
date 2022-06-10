package com.islom.invoked.invoked

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel


class MainActivity : FlutterActivity() {
    private val BatteryChannel = "batteryChannel"
    private val EventChannel = "eventChannel"
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var bluetoothLeScanner  :  BluetoothAdapter? = null
    private var bleutz  :  BluetoothLeScanner? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    var scanSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
            .setReportDelay(0L)
            .build()
    } else {
        TODO("VERSION.SDK_INT < M")
    }
    private val TAG = MainActivity::class.java.simpleName
    var filters: List<ScanFilter>? = null
    var hi: BluetoothDevice? =null


    @SuppressLint("MissingPermission")
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {

        super.configureFlutterEngine(flutterEngine)

         val scanCallback: ScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                hi=device
                Log.i(TAG, "result :  ${device}")
                print( "result :  ${device}")

                // ...do whatever you want with this found device
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                // Ignore for now
            }

            override fun onScanFailed(errorCode: Int) {
                // Ignore for now
            }
        }
        val adapter =  BluetoothAdapter.getDefaultAdapter()
        val scanner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            adapter.bluetoothLeScanner

        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP")
        }

        Log.i(TAG, "scanner :  ${scanner}")

        if (scanner != null) {

            scanner.startScan(filters,scanSettings,scanCallback )
        } else {
            Log.e(TAG, "could not get scanner object")
        }
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, BatteryChannel)
        eventChannel = EventChannel(flutterEngine.dartExecutor.binaryMessenger, EventChannel)
        channel.setMethodCallHandler { call, result ->
            if (call.method == "getBatteryLevel") {
                val arguments = call.arguments<Map<String, String>>() as Map<String, String>
                val name = arguments["name"]
                if(hi!=null){
                    Log.i(TAG, "hello ${hi}")
                }else{
                    Log.i(TAG, "null  chqvotti")
                }
                var batteryLevel = getBatteryLevel()
                result.success(batteryLevel)

            }

        }
        eventChannel.setStreamHandler(MyStreamHandler(context))

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
//        val batteryLevel=getBatteryLevel()
//        channel.invokeMethod("reportBattery",batteryLevel)
        Handler(Looper.getMainLooper()).postDelayed({
            val batteryLevel = getBatteryLevel()
            channel.invokeMethod("reportBattery", batteryLevel)

        }, 1)

    }

    private fun getBatteryLevel(): Int {
        val batteryLevel: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } else {
            val intent = ContextWrapper(applicationContext).registerReceiver(null, IntentFilter())
            batteryLevel = intent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100

        }
        return batteryLevel
    }


}


class MyStreamHandler(private val context: Context) : EventChannel.StreamHandler {
    private var receiver: BroadcastReceiver? = null
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {

        if (events == null) return

        receiver = initReciever(events)
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    }

    override fun onCancel(arguments: Any?) {

        context.unregisterReceiver(receiver)
        receiver = null
    }


    private fun initReciever(events: EventChannel.EventSink): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                when (status) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> events.success("Battery Is Charging")
                    BatteryManager.BATTERY_STATUS_FULL -> events.success("Battery Is Full")
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> events.success("Battery Is Discharge")
                }
            }
        }
    }
}