package com.example.chianiuprinter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class ForegroundService : Service() {
    
    companion object {
        private const val TAG = "ForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "print_service_channel"
        private const val CHANNEL_NAME = "打印服务"
        
        // Action常量
        const val ACTION_START = "com.example.chianiuprinter.ACTION_START"
        const val ACTION_STOP = "com.example.chianiuprinter.ACTION_STOP"
        const val ACTION_PRINT_TEST = "com.example.chianiuprinter.ACTION_PRINT_TEST"
    }
    
    private lateinit var notificationManager: NotificationManager
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "前台服务创建")
        
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "前台服务启动命令")
        
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                startForegroundService()
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
            ACTION_PRINT_TEST -> {
                handleTestPrint()
            }
            else -> {
                startForegroundService()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "前台服务销毁")
        
        // 停止前台服务
        stopForeground(true)
    }
    
    private fun startForegroundService() {
        Log.d(TAG, "启动前台服务")
        
        // 创建通知
        val notification = createNotification()
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, notification)
        
        // 发送广播通知服务已启动
        sendBroadcast(Intent(ACTION_SERVICE_STARTED))
        
        Log.d(TAG, "前台服务已启动")
    }
    
    private fun stopForegroundService() {
        Log.d(TAG, "停止前台服务")
        
        // 停止前台服务
        stopForeground(true)
        stopSelf()
        
        // 发送广播通知服务已停止
        sendBroadcast(Intent(ACTION_SERVICE_STOPPED))
        
        Log.d(TAG, "前台服务已停止")
    }
    
    private fun handleTestPrint() {
        Log.d(TAG, "处理测试打印")
        
        // 在后台线程执行测试打印
        Thread {
            if (BluetoothPrintManager.isConnected()) {
                val success = BluetoothPrintManager.printTestPage()
                
                // 更新通知显示结果
                updateNotification(
                    if (success) "测试打印成功" else "测试打印失败"
                )
            } else {
                updateNotification("打印机未连接")
            }
        }.start()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示打印服务的运行状态"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // 创建点击通知时打开的Intent
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建停止服务的Intent
        val stopIntent = Intent(this, ForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 创建测试打印的Intent
        val testPrintIntent = Intent(this, ForegroundService::class.java).apply {
            action = ACTION_PRINT_TEST
        }
        val testPrintPendingIntent = PendingIntent.getService(
            this,
            2,
            testPrintIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("牵牛花打印助手")
            .setContentText("正在监听牵牛花App")
            .setSmallIcon(R.drawable.ic_printer)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            
            // 添加操作按钮
            .addAction(
                R.drawable.ic_print,
                "测试打印",
                testPrintPendingIntent
            )
            .addAction(
                R.drawable.ic_stop,
                "停止服务",
                stopPendingIntent
            )
            
            .build()
    }
    
    private fun updateNotification(message: String) {
        val notification = createNotification().apply {
            // 更新内容文本
            val style = NotificationCompat.BigTextStyle()
                .bigText("正在监听牵牛花App\n$message")
            
            (this as NotificationCompat.Builder).setStyle(style)
        }
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    // 广播Action常量
    companion object {
        const val ACTION_SERVICE_STARTED = "com.example.chianiuprinter.SERVICE_STARTED"
        const val ACTION_SERVICE_STOPPED = "com.example.chianiuprinter.SERVICE_STOPPED"
        const val ACTION_ACCESSIBILITY_SERVICE_STARTED = "com.example.chianiuprinter.ACCESSIBILITY_SERVICE_STARTED"
    }
}