package com.example.chianiuprinter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "系统启动完成")
                handleBootCompleted(context)
            }
            
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d(TAG, "锁屏启动完成")
                handleBootCompleted(context)
            }
            
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "应用更新完成")
                handleAppUpdated(context)
            }
            
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "快速启动完成")
                handleBootCompleted(context)
            }
        }
    }
    
    /**
     * 处理系统启动完成
     */
    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "处理系统启动")
        
        // 检查是否启用开机自启
        if (shouldAutoStart(context)) {
            Log.d(TAG, "启用开机自启，启动服务")
            startServices(context)
        } else {
            Log.d(TAG, "开机自启未启用")
        }
        
        // 检查是否自动连接打印机
        if (shouldAutoConnect(context)) {
            Log.d(TAG, "启用自动连接打印机")
            // 这里可以添加自动连接打印机的逻辑
        }
    }
    
    /**
     * 处理应用更新完成
     */
    private fun handleAppUpdated(context: Context) {
        Log.d(TAG, "处理应用更新")
        
        // 应用更新后可能需要重新请求权限或初始化
        // 这里可以添加更新后的初始化逻辑
        
        // 检查服务是否需要重启
        if (wasServiceRunningBeforeUpdate(context)) {
            Log.d(TAG, "应用更新前服务在运行，尝试重启服务")
            startServices(context)
        }
    }
    
    /**
     * 启动服务
     */
    private fun startServices(context: Context) {
        try {
            // 启动前台服务
            val serviceIntent = Intent(context, ForegroundService::class.java).apply {
                action = ForegroundService.ACTION_START
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "服务启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "启动服务失败", e)
        }
    }
    
    /**
     * 检查是否应该开机自启
     */
    private fun shouldAutoStart(context: Context): Boolean {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_start", false)
    }
    
    /**
     * 检查是否应该自动连接打印机
     */
    private fun shouldAutoConnect(context: Context): Boolean {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_connect", true)
    }
    
    /**
     * 检查应用更新前服务是否在运行
     */
    private fun wasServiceRunningBeforeUpdate(context: Context): Boolean {
        // 这里可以添加逻辑来检查更新前服务状态
        // 例如保存服务状态到SharedPreferences
        val prefs = context.getSharedPreferences("service_state", Context.MODE_PRIVATE)
        return prefs.getBoolean("was_running", false)
    }
    
    /**
     * 保存服务运行状态
     */
    fun saveServiceRunningState(context: Context, isRunning: Boolean) {
        val prefs = context.getSharedPreferences("service_state", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("was_running", isRunning)
            .apply()
    }
    
    /**
     * 处理其他广播
     */
    fun handleOtherBroadcasts(context: Context, intent: Intent) {
        when (intent.action) {
            // 蓝牙状态变化
            android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(
                    android.bluetooth.BluetoothAdapter.EXTRA_STATE,
                    android.bluetooth.BluetoothAdapter.ERROR
                )
                
                when (state) {
                    android.bluetooth.BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "蓝牙已开启")
                        handleBluetoothEnabled(context)
                    }
                    
                    android.bluetooth.BluetoothAdapter.STATE_OFF -> {
                        Log.d(TAG, "蓝牙已关闭")
                        handleBluetoothDisabled(context)
                    }
                }
            }
            
            // 网络状态变化
            android.net.ConnectivityManager.CONNECTIVITY_ACTION -> {
                Log.d(TAG, "网络状态变化")
                // 这里可以添加网络状态变化的处理逻辑
            }
            
            // 电池状态变化
            Intent.ACTION_BATTERY_CHANGED -> {
                // 这里可以添加电池状态变化的处理逻辑
                // 例如低电量时停止某些功能
            }
            
            // 时区变化
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d(TAG, "时区变化")
                // 更新时间相关设置
            }
            
            // 日期变化
            Intent.ACTION_DATE_CHANGED -> {
                Log.d(TAG, "日期变化")
                // 处理日期变化，例如清理旧数据
                cleanupOldData(context)
            }
        }
    }
    
    /**
     * 处理蓝牙启用
     */
    private fun handleBluetoothEnabled(context: Context) {
        // 蓝牙启用后，如果设置了自动连接，尝试连接打印机
        if (shouldAutoConnect(context)) {
            Log.d(TAG, "蓝牙已启用，尝试自动连接打印机")
            // 这里可以添加自动连接打印机的逻辑
        }
    }
    
    /**
     * 处理蓝牙禁用
     */
    private fun handleBluetoothDisabled(context: Context) {
        Log.d(TAG, "蓝牙已禁用，断开打印机连接")
        BluetoothPrintManager.disconnect()
    }
    
    /**
     * 清理旧数据
     */
    private fun cleanupOldData(context: Context) {
        Log.d(TAG, "清理旧数据")
        
        // 这里可以添加清理旧数据的逻辑
        // 例如：删除30天前的打印记录
        
        val prefs = context.getSharedPreferences("cleanup", Context.MODE_PRIVATE)
        val lastCleanup = prefs.getLong("last_cleanup", 0)
        val currentTime = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        // 每天只清理一次
        if (currentTime - lastCleanup > oneDay) {
            // 执行清理逻辑
            cleanupOldPrintRecords(context)
            
            // 更新最后清理时间
            prefs.edit()
                .putLong("last_cleanup", currentTime)
                .apply()
        }
    }
    
    /**
     * 清理旧的打印记录
     */
    private fun cleanupOldPrintRecords(context: Context) {
        // 获取30天前的时间戳
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        
        // 获取所有记录
        val allRecords = PrintHistoryManager.getPrintHistory(context)
        
        // 筛选30天前的记录
        val oldRecords = allRecords.filter { it.printTime < thirtyDaysAgo }
        
        if (oldRecords.isNotEmpty()) {
            Log.d(TAG, "清理 ${oldRecords.size} 条旧记录")
            
            // 删除旧记录
            for (record in oldRecords) {
                PrintHistoryManager.deletePrintRecord(context, record.id)
            }
        }
    }
}