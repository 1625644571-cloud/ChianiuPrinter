package com.example.chianiuprinter

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityManager

class ServiceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ServiceManager"
        
        // 无障碍服务ID
        private const val ACCESSIBILITY_SERVICE_ID = "com.example.chianiuprinter/.ChianiuAccessibilityService"
    }
    
    /**
     * 检查前台服务是否正在运行
     */
    fun isServiceRunning(): Boolean {
        return isForegroundServiceRunning()
    }
    
    /**
     * 检查无障碍服务是否已启用
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return isAccessibilityServiceEnabled(context, ACCESSIBILITY_SERVICE_ID)
    }
    
    /**
     * 检查前台服务是否运行
     */
    private fun isForegroundServiceRunning(): Boolean {
        // 这里可以添加更精确的检查逻辑
        // 目前使用简单的广播接收方式
        return false // 临时返回false，实际应用中需要实现
    }
    
    /**
     * 检查无障碍服务是否启用
     */
    private fun isAccessibilityServiceEnabled(
        context: Context,
        serviceId: String
    ): Boolean {
        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            if (enabledServices == null) {
                return false
            }
            
            val enabledServicesList = TextUtils.split(enabledServices, ":")
            for (enabledService in enabledServicesList) {
                if (enabledService.equals(serviceId, ignoreCase = true)) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查无障碍服务状态失败", e)
        }
        
        return false
    }
    
    /**
     * 获取无障碍服务信息
     */
    fun getAccessibilityServiceInfo(): AccessibilityServiceInfo? {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            
            for (service in enabledServices) {
                if (service.id.contains("ChianiuAccessibilityService")) {
                    return service
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "获取无障碍服务信息失败", e)
            null
        }
    }
    
    /**
     * 启动所有服务
     */
    fun startAllServices() {
        // 启动前台服务
        startForegroundService()
        
        // 检查并提示开启无障碍服务
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityServicePrompt()
        }
    }
    
    /**
     * 停止所有服务
     */
    fun stopAllServices() {
        // 停止前台服务
        stopForegroundService()
    }
    
    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        val intent = Intent(context, ForegroundService::class.java).apply {
            action = ForegroundService.ACTION_START
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    /**
     * 停止前台服务
     */
    private fun stopForegroundService() {
        val intent = Intent(context, ForegroundService::class.java).apply {
            action = ForegroundService.ACTION_STOP
        }
        
        context.stopService(intent)
    }
    
    /**
     * 显示开启无障碍服务的提示
     */
    private fun showAccessibilityServicePrompt() {
        // 这里可以显示对话框或Toast提示用户
        // 实际应用中应该引导用户去设置页面
    }
    
    /**
     * 检查所有必要权限
     */
    fun checkAllPermissions(): Boolean {
        // 检查蓝牙权限
        val hasBluetoothPermission = PermissionUtils.hasBluetoothPermissions(context)
        
        // 检查位置权限（Android 6.0+需要位置权限来搜索蓝牙设备）
        val hasLocationPermission = PermissionUtils.hasLocationPermission(context)
        
        // 检查无障碍服务
        val hasAccessibilityService = isAccessibilityServiceEnabled()
        
        return hasBluetoothPermission && hasLocationPermission && hasAccessibilityService
    }
    
    /**
     * 获取服务状态摘要
     */
    fun getServiceStatusSummary(): String {
        val status = StringBuilder()
        
        // 前台服务状态
        status.append("前台服务: ")
        status.append(if (isServiceRunning()) "运行中" else "已停止")
        status.append("\n")
        
        // 无障碍服务状态
        status.append("无障碍服务: ")
        status.append(if (isAccessibilityServiceEnabled()) "已启用" else "未启用")
        status.append("\n")
        
        // 打印机状态
        status.append("打印机: ")
        status.append(BluetoothPrintManager.getPrinterInfo())
        
        return status.toString()
    }
}