package com.example.chianiuprinter

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_ENABLE_BLUETOOTH = 1
        private const val REQUEST_ACCESSIBILITY_SETTINGS = 2
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 3
        private const val REQUEST_LOCATION_PERMISSION = 4
    }
    
    // UI组件
    private lateinit var tvServiceStatus: TextView
    private lateinit var ivServiceStatus: ImageView
    private lateinit var btnToggleService: Button
    
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var ivAccessibilityStatus: ImageView
    private lateinit var btnEnableAccessibility: Button
    
    private lateinit var tvPrinterStatus: TextView
    private lateinit var ivPrinterStatus: ImageView
    private lateinit var btnConnectPrinter: Button
    private lateinit var btnTestPrint: Button
    
    private lateinit var btnSettings: Button
    private lateinit var btnHistory: Button
    private lateinit var tvLastPrint: TextView
    
    // 蓝牙适配器
    private lateinit var bluetoothAdapter: BluetoothAdapter
    
    // 服务管理
    private lateinit var serviceManager: ServiceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // 初始化UI组件
        initViews()
        
        // 初始化蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        
        // 初始化服务管理器
        serviceManager = ServiceManager(this)
        
        // 设置点击监听器
        setupClickListeners()
        
        // 检查权限
        checkPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        // 更新UI状态
        updateServiceStatus()
        updateAccessibilityStatus()
        updatePrinterStatus()
        updateLastPrintInfo()
    }
    
    private fun initViews() {
        tvServiceStatus = findViewById(R.id.tv_service_status)
        ivServiceStatus = findViewById(R.id.iv_service_status)
        btnToggleService = findViewById(R.id.btn_toggle_service)
        
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status)
        ivAccessibilityStatus = findViewById(R.id.iv_accessibility_status)
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility)
        
        tvPrinterStatus = findViewById(R.id.tv_printer_status)
        ivPrinterStatus = findViewById(R.id.iv_printer_status)
        btnConnectPrinter = findViewById(R.id.btn_connect_printer)
        btnTestPrint = findViewById(R.id.btn_test_print)
        
        btnSettings = findViewById(R.id.btn_settings)
        btnHistory = findViewById(R.id.btn_history)
        tvLastPrint = findViewById(R.id.tv_last_print)
    }
    
    private fun setupClickListeners() {
        // 服务开关
        btnToggleService.setOnClickListener {
            if (serviceManager.isServiceRunning()) {
                stopService()
            } else {
                startService()
            }
        }
        
        // 开启无障碍服务
        btnEnableAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }
        
        // 连接打印机
        btnConnectPrinter.setOnClickListener {
            if (!checkBluetoothPermissions()) {
                requestBluetoothPermissions()
                return@setOnClickListener
            }
            
            if (!isBluetoothEnabled()) {
                enableBluetooth()
                return@setOnClickListener
            }
            
            connectToPrinter()
        }
        
        // 测试打印
        btnTestPrint.setOnClickListener {
            if (BluetoothPrintManager.isConnected()) {
                testPrint()
            } else {
                Toast.makeText(this, "请先连接打印机", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 设置
        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // 打印历史
        btnHistory.setOnClickListener {
            val intent = Intent(this, PrintHistoryActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun checkPermissions() {
        // 检查蓝牙权限
        checkBluetoothPermissions()
        
        // 检查位置权限（Android 6.0+需要位置权限来搜索蓝牙设备）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
                )
            }
        }
    }
    
    private fun checkBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }
    
    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        }
    }
    
    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
    
    private fun enableBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
        }
    }
    
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, REQUEST_ACCESSIBILITY_SETTINGS)
    }
    
    private fun startService() {
        if (!serviceManager.isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
            openAccessibilitySettings()
            return
        }
        
        val intent = Intent(this, ForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        updateServiceStatus()
        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopService() {
        val intent = Intent(this, ForegroundService::class.java)
        stopService(intent)
        
        updateServiceStatus()
        Toast.makeText(this, "服务已停止", Toast.LENGTH_SHORT).show()
    }
    
    private fun connectToPrinter() {
        // 显示打印机选择对话框
        showPrinterSelectionDialog()
    }
    
    private fun showPrinterSelectionDialog() {
        if (!isBluetoothEnabled()) {
            Toast.makeText(this, "请先开启蓝牙", Toast.LENGTH_SHORT).show()
            return
        }
        
        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val printerDevices = mutableListOf<BluetoothDevice>()
        val deviceNames = mutableListOf<String>()
        
        // 过滤出德佟P1打印机（根据名称判断）
        for (device in pairedDevices) {
            if (device.name.contains("德佟", ignoreCase = true) || 
                device.name.contains("Detong", ignoreCase = true) ||
                device.name.contains("P1", ignoreCase = true)) {
                printerDevices.add(device)
                deviceNames.add("${device.name}\n${device.address}")
            }
        }
        
        if (printerDevices.isEmpty()) {
            // 如果没有已配对的打印机，显示搜索对话框
            showSearchPrinterDialog()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("选择打印机")
            .setItems(deviceNames.toTypedArray()) { dialog, which ->
                val selectedDevice = printerDevices[which]
                connectToDevice(selectedDevice)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showSearchPrinterDialog() {
        AlertDialog.Builder(this)
            .setTitle("未找到打印机")
            .setMessage("未找到已配对的德佟P1打印机。\n\n请确保：\n1. 打印机已开机\n2. 打印机处于配对模式\n3. 在系统蓝牙设置中配对打印机")
            .setPositiveButton("去配对") { dialog, which ->
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        btnConnectPrinter.text = getString(R.string.connecting)
        btnConnectPrinter.isEnabled = false
        
        // 在后台线程连接打印机
        Thread {
            val success = BluetoothPrintManager.connect(device)
            
            runOnUiThread {
                btnConnectPrinter.isEnabled = true
                
                if (success) {
                    btnConnectPrinter.text = getString(R.string.disconnect_printer)
                    btnTestPrint.isEnabled = true
                    updatePrinterStatus()
                    Toast.makeText(this, "打印机连接成功", Toast.LENGTH_SHORT).show()
                } else {
                    btnConnectPrinter.text = getString(R.string.connect_printer)
                    Toast.makeText(this, "连接失败，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun testPrint() {
        val testData = PrintData(
            orderNumber = "TEST-20250326-001",
            productName = "测试商品",
            storageLocation = "TEST-1-1",
            quantity = "1件",
            printTime = System.currentTimeMillis()
        )
        
        Thread {
            val success = BluetoothPrintManager.printLabel(testData)
            
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "测试打印成功", Toast.LENGTH_SHORT).show()
                    updateLastPrintInfo()
                } else {
                    Toast.makeText(this, "测试打印失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    
    private fun updateServiceStatus() {
        val isRunning = serviceManager.isServiceRunning()
        
        if (isRunning) {
            tvServiceStatus.text = getString(R.string.service_running)
            ivServiceStatus.setImageResource(R.drawable.ic_service_on)
            btnToggleService.text = getString(R.string.stop_service)
        } else {
            tvServiceStatus.text = getString(R.string.service_stopped)
            ivServiceStatus.setImageResource(R.drawable.ic_service_off)
            btnToggleService.text = getString(R.string.start_service)
        }
    }
    
    private fun updateAccessibilityStatus() {
        val isEnabled = serviceManager.isAccessibilityServiceEnabled()
        
        if (isEnabled) {
            tvAccessibilityStatus.text = "无障碍服务已开启"
            tvAccessibilityStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            ivAccessibilityStatus.setImageResource(R.drawable.ic_check)
            btnEnableAccessibility.visibility = View.GONE
        } else {
            tvAccessibilityStatus.text = getString(R.string.accessibility_service_not_enabled)
            tvAccessibilityStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
            ivAccessibilityStatus.setImageResource(R.drawable.ic_warning)
            btnEnableAccessibility.visibility = View.VISIBLE
        }
    }
    
    private fun updatePrinterStatus() {
        val isConnected = BluetoothPrintManager.isConnected()
        
        if (isConnected) {
            tvPrinterStatus.text = getString(R.string.printer_connected)
            ivPrinterStatus.setImageResource(R.drawable.ic_bluetooth_connected)
            btnConnectPrinter.text = getString(R.string.disconnect_printer)
            btnTestPrint.isEnabled = true
        } else {
            tvPrinterStatus.text = getString(R.string.printer_disconnected)
            ivPrinterStatus.setImageResource(R.drawable.ic_bluetooth_off)
            btnConnectPrinter.text = getString(R.string.connect_printer)
            btnTestPrint.isEnabled = false
        }
    }
    
    private fun updateLastPrintInfo() {
        val lastPrint = PrintHistoryManager.getLastPrint(this)
        if (lastPrint != null) {
            val time = DateUtils.formatDateTime(lastPrint.printTime)
            tvLastPrint.text = "上次打印: $time (${lastPrint.productName})"
        } else {
            tvLastPrint.text = "暂无打印记录"
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_ENABLE_BLUETOOTH -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show()
                    updatePrinterStatus()
                } else {
                    Toast.makeText(this, "蓝牙未开启", Toast.LENGTH_SHORT).show()
                }
            }
            
            REQUEST_ACCESSIBILITY_SETTINGS -> {
                updateAccessibilityStatus()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "蓝牙权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要蓝牙权限才能连接打印机", Toast.LENGTH_SHORT).show()
                }
            }
            
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "位置权限已授予", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "需要位置权限来搜索蓝牙设备", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}