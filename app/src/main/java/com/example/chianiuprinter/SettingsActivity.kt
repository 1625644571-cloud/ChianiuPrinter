package com.example.chianiuprinter

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class SettingsActivity : AppCompatActivity() {
    
    // UI组件
    private lateinit var tvAppVersion: TextView
    private lateinit var tvDeviceInfo: TextView
    private lateinit var tvPermissionStatus: TextView
    private lateinit var tvServiceStatus: TextView
    
    private lateinit var cbAutoStart: CheckBox
    private lateinit var cbAutoConnect: CheckBox
    private lateinit var cbVibrate: CheckBox
    private lateinit var cbSound: CheckBox
    
    private lateinit var btnClearHistory: Button
    private lateinit var btnExportHistory: Button
    private lateinit var btnAppSettings: Button
    private lateinit var btnAbout: Button
    private lateinit var btnBack: Button
    
    // 服务管理器
    private lateinit var serviceManager: ServiceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // 初始化UI组件
        initViews()
        
        // 初始化服务管理器
        serviceManager = ServiceManager(this)
        
        // 设置点击监听器
        setupClickListeners()
        
        // 加载设置
        loadSettings()
        
        // 更新状态显示
        updateStatusDisplay()
    }
    
    override fun onResume() {
        super.onResume()
        // 更新状态显示
        updateStatusDisplay()
    }
    
    private fun initViews() {
        tvAppVersion = findViewById(R.id.tv_app_version)
        tvDeviceInfo = findViewById(R.id.tv_device_info)
        tvPermissionStatus = findViewById(R.id.tv_permission_status)
        tvServiceStatus = findViewById(R.id.tv_service_status)
        
        cbAutoStart = findViewById(R.id.cb_auto_start)
        cbAutoConnect = findViewById(R.id.cb_auto_connect)
        cbVibrate = findViewById(R.id.cb_vibrate)
        cbSound = findViewById(R.id.cb_sound)
        
        btnClearHistory = findViewById(R.id.btn_clear_history)
        btnExportHistory = findViewById(R.id.btn_export_history)
        btnAppSettings = findViewById(R.id.btn_app_settings)
        btnAbout = findViewById(R.id.btn_about)
        btnBack = findViewById(R.id.btn_back)
    }
    
    private fun setupClickListeners() {
        // 自动启动
        cbAutoStart.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("auto_start", isChecked)
        }
        
        // 自动连接
        cbAutoConnect.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("auto_connect", isChecked)
        }
        
        // 振动提示
        cbVibrate.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("vibrate", isChecked)
        }
        
        // 声音提示
        cbSound.setOnCheckedChangeListener { _, isChecked ->
            saveSetting("sound", isChecked)
        }
        
        // 清空历史
        btnClearHistory.setOnClickListener {
            showClearHistoryDialog()
        }
        
        // 导出历史
        btnExportHistory.setOnClickListener {
            exportHistory()
        }
        
        // 应用设置
        btnAppSettings.setOnClickListener {
            openAppSettings()
        }
        
        // 关于
        btnAbout.setOnClickListener {
            showAboutDialog()
        }
        
        // 返回
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        
        cbAutoStart.isChecked = prefs.getBoolean("auto_start", false)
        cbAutoConnect.isChecked = prefs.getBoolean("auto_connect", true)
        cbVibrate.isChecked = prefs.getBoolean("vibrate", true)
        cbSound.isChecked = prefs.getBoolean("sound", true)
    }
    
    private fun saveSetting(key: String, value: Boolean) {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit()
            .putBoolean(key, value)
            .apply()
    }
    
    private fun updateStatusDisplay() {
        // 应用版本
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        
        tvAppVersion.text = "版本: $versionName ($versionCode)"
        
        // 设备信息
        val deviceInfo = StringBuilder()
        deviceInfo.append("设备: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        deviceInfo.append("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        deviceInfo.append("处理器: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
        
        tvDeviceInfo.text = deviceInfo.toString()
        
        // 权限状态
        tvPermissionStatus.text = PermissionUtils.getPermissionStatusSummary(this)
        
        // 服务状态
        tvServiceStatus.text = serviceManager.getServiceStatusSummary()
    }
    
    private fun showClearHistoryDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("清空打印历史")
            .setMessage("确定要清空所有打印历史记录吗？此操作不可恢复。")
            .setPositiveButton("清空") { dialog, which ->
                PrintHistoryManager.clearPrintHistory(this)
                showToast("打印历史已清空")
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun exportHistory() {
        // 导出CSV格式
        val csvContent = PrintHistoryManager.exportToCsv(this)
        
        // 这里可以添加保存到文件或分享的逻辑
        // 例如：保存到Downloads文件夹或通过Intent分享
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("导出历史")
            .setMessage("已生成 ${csvContent.lines().size - 1} 条记录\n\n选择导出方式：")
            .setPositiveButton("复制到剪贴板") { dialog, which ->
                copyToClipboard(csvContent)
                showToast("已复制到剪贴板")
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("打印历史", text)
        clipboard.setPrimaryClip(clip)
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
    
    private fun showAboutDialog() {
        val aboutText = """
            牵牛花打印助手
            
            版本: ${packageManager.getPackageInfo(packageName, 0).versionName}
            
            功能:
            • 监听牵牛花App收货确认操作
            • 自动提取商品信息
            • 蓝牙连接德佟P1打印机
            • 自动打印商品标签
            
            技术支持:
            • 需要Android 8.0以上系统
            • 需要开启无障碍服务
            • 需要蓝牙权限
            
            免责声明:
            本应用仅用于辅助仓储管理，不存储任何用户敏感信息。
            使用前请确保已获得相关授权。
            
            © 2025 牵牛花打印助手
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("关于")
            .setMessage(aboutText)
            .setPositiveButton("确定", null)
            .show()
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}