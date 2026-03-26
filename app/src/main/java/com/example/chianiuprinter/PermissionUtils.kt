package com.example.chianiuprinter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {
    
    /**
     * 检查蓝牙权限（Android 12+需要新的权限）
     */
    fun hasBluetoothPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要 BLUETOOTH_CONNECT 和 BLUETOOTH_SCAN 权限
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        } else {
            // Android 11及以下需要基本的蓝牙权限
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) 
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) 
                != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 检查位置权限（搜索蓝牙设备需要位置权限）
     */
    fun hasLocationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ 需要位置权限来搜索蓝牙设备
            val fineLocation = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val coarseLocation = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            return fineLocation || coarseLocation
        }
        
        // Android 5.1及以下不需要运行时位置权限
        return true
    }
    
    /**
     * 检查前台服务权限
     */
    fun hasForegroundServicePermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9.0+ 需要前台服务权限
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        }
        
        return true
    }
    
    /**
     * 检查所有必要权限
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasBluetoothPermissions(context) && 
               hasLocationPermission(context) &&
               hasForegroundServicePermission(context)
    }
    
    /**
     * 获取缺失的权限列表
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missingPermissions = mutableListOf<String>()
        
        // 检查蓝牙权限
        if (!hasBluetoothPermissions(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                missingPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                missingPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            } else {
                missingPermissions.add(Manifest.permission.BLUETOOTH)
                missingPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
        
        // 检查位置权限
        if (!hasLocationPermission(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        
        // 检查前台服务权限
        if (!hasForegroundServicePermission(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                missingPermissions.add(Manifest.permission.FOREGROUND_SERVICE)
            }
        }
        
        return missingPermissions
    }
    
    /**
     * 获取权限说明
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.BLUETOOTH_CONNECT -> "蓝牙连接权限：用于连接打印机"
            Manifest.permission.BLUETOOTH_SCAN -> "蓝牙扫描权限：用于搜索打印机"
            Manifest.permission.BLUETOOTH -> "蓝牙权限：使用蓝牙功能"
            Manifest.permission.BLUETOOTH_ADMIN -> "蓝牙管理权限：管理蓝牙连接"
            Manifest.permission.ACCESS_FINE_LOCATION -> "精确定位权限：搜索蓝牙设备需要"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "粗略定位权限：搜索蓝牙设备需要"
            Manifest.permission.FOREGROUND_SERVICE -> "前台服务权限：保持应用在后台运行"
            else -> "未知权限"
        }
    }
    
    /**
     * 检查是否支持蓝牙
     */
    fun isBluetoothSupported(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }
    
    /**
     * 检查是否支持蓝牙低功耗（BLE）
     */
    fun isBluetoothLESupported(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }
    
    /**
     * 获取权限状态摘要
     */
    fun getPermissionStatusSummary(context: Context): String {
        val summary = StringBuilder()
        
        // 蓝牙支持
        summary.append("蓝牙支持: ")
        summary.append(if (isBluetoothSupported(context)) "是" else "否")
        summary.append("\n")
        
        // 蓝牙权限
        summary.append("蓝牙权限: ")
        summary.append(if (hasBluetoothPermissions(context)) "已授予" else "未授予")
        summary.append("\n")
        
        // 位置权限
        summary.append("位置权限: ")
        summary.append(if (hasLocationPermission(context)) "已授予" else "未授予")
        summary.append("\n")
        
        // 前台服务权限
        summary.append("前台服务: ")
        summary.append(if (hasForegroundServicePermission(context)) "已授予" else "未授予")
        
        return summary.toString()
    }
}