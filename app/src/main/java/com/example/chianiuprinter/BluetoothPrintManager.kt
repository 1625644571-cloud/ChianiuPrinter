package com.example.chianiuprinter

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object BluetoothPrintManager {
    
    companion object {
        private const val TAG = "BluetoothPrint"
        
        // 德佟P1打印机使用的UUID
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        // ESC/POS 命令常量
        private const val ESC = 0x1B // Escape
        private const val GS = 0x1D // Group Separator
        private const val LF = 0x0A // Line Feed
        
        // 打印机状态
        private var isConnected = false
        private var bluetoothSocket: BluetoothSocket? = null
        private var outputStream: OutputStream? = null
        
        // 打印机信息
        private var printerName: String = ""
        private var printerAddress: String = ""
    }
    
    /**
     * 连接蓝牙打印机
     */
    fun connect(device: BluetoothDevice): Boolean {
        disconnect() // 先断开现有连接
        
        return try {
            Log.d(TAG, "正在连接打印机: ${device.name} (${device.address})")
            
            // 创建蓝牙Socket
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            
            // 连接
            socket.connect()
            
            // 获取输出流
            val stream = socket.outputStream
            
            // 保存连接信息
            bluetoothSocket = socket
            outputStream = stream
            printerName = device.name ?: "未知打印机"
            printerAddress = device.address
            isConnected = true
            
            Log.d(TAG, "打印机连接成功")
            
            // 发送初始化命令
            initializePrinter()
            
            true
        } catch (e: IOException) {
            Log.e(TAG, "连接打印机失败", e)
            disconnect()
            false
        } catch (e: SecurityException) {
            Log.e(TAG, "蓝牙权限不足", e)
            disconnect()
            false
        }
    }
    
    /**
     * 断开打印机连接
     */
    fun disconnect() {
        try {
            outputStream?.close()
        } catch (e: IOException) {
            Log.e(TAG, "关闭输出流失败", e)
        }
        
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "关闭Socket失败", e)
        }
        
        outputStream = null
        bluetoothSocket = null
        isConnected = false
        printerName = ""
        printerAddress = ""
        
        Log.d(TAG, "打印机已断开连接")
    }
    
    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean {
        return isConnected && bluetoothSocket?.isConnected == true
    }
    
    /**
     * 获取打印机信息
     */
    fun getPrinterInfo(): String {
        return if (isConnected()) {
            "$printerName ($printerAddress)"
        } else {
            "未连接"
        }
    }
    
    /**
     * 打印商品标签
     */
    fun printLabel(printData: PrintData): Boolean {
        if (!isConnected()) {
            Log.e(TAG, "打印机未连接")
            return false
        }
        
        return try {
            Log.d(TAG, "开始打印: ${printData.productName}")
            
            // 初始化打印机
            initializePrinter()
            
            // 设置对齐方式：居中
            writeBytes(byteArrayOf(ESC, 0x61, 0x01))
            
            // 设置字体大小：双倍宽高
            writeBytes(byteArrayOf(GS, 0x21, 0x11))
            
            // 打印标题
            printLine("收货标签")
            printLine("==========")
            
            // 换行
            feedLine(1)
            
            // 恢复默认字体
            writeBytes(byteArrayOf(GS, 0x21, 0x00))
            
            // 左对齐
            writeBytes(byteArrayOf(ESC, 0x61, 0x00))
            
            // 打印详细信息
            printLine("收货单号: ${printData.orderNumber}")
            printLine("商品名称: ${printData.productName}")
            printLine("入库库位: ${printData.storageLocation}")
            printLine("数量: ${printData.quantity}")
            
            // 换行
            feedLine(1)
            
            // 打印时间
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            val printTime = timeFormat.format(Date(printData.printTime))
            printLine("打印时间: $printTime")
            
            // 换行
            feedLine(2)
            
            // 打印分隔线
            printLine("----------------")
            
            // 切纸（如果有自动切刀）
            cutPaper()
            
            // 送纸
            feedLine(3)
            
            Log.d(TAG, "打印完成: ${printData.productName}")
            true
            
        } catch (e: IOException) {
            Log.e(TAG, "打印失败", e)
            disconnect() // 断开连接，下次需要重新连接
            false
        } catch (e: Exception) {
            Log.e(TAG, "打印时发生未知错误", e)
            false
        }
    }
    
    /**
     * 打印测试页
     */
    fun printTestPage(): Boolean {
        if (!isConnected()) {
            return false
        }
        
        return try {
            initializePrinter()
            
            // 居中
            writeBytes(byteArrayOf(ESC, 0x61, 0x01))
            
            // 大字体
            writeBytes(byteArrayOf(GS, 0x21, 0x22))
            printLine("测试打印")
            writeBytes(byteArrayOf(GS, 0x21, 0x00))
            
            feedLine(1)
            
            // 左对齐
            writeBytes(byteArrayOf(ESC, 0x61, 0x00))
            
            printLine("打印机: $printerName")
            printLine("地址: $printerAddress")
            printLine("状态: 正常")
            
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
            val currentTime = timeFormat.format(Date())
            printLine("时间: $currentTime")
            
            feedLine(2)
            printLine("德佟P1打印机测试")
            printLine("ESC/POS 协议")
            
            feedLine(3)
            printLine("----------------")
            
            // 打印二维码测试（如果有支持）
            printQRCode("TEST-123456")
            
            feedLine(3)
            cutPaper()
            feedLine(3)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "打印测试页失败", e)
            false
        }
    }
    
    /**
     * 初始化打印机
     */
    private fun initializePrinter() {
        try {
            // 初始化命令
            writeBytes(byteArrayOf(ESC, 0x40))
            
            // 设置字符编码为GBK（中文）
            writeBytes(byteArrayOf(ESC, 0x74, 0x0F))
            
            Thread.sleep(100) // 短暂延迟
        } catch (e: Exception) {
            Log.e(TAG, "初始化打印机失败", e)
        }
    }
    
    /**
     * 打印一行文本
     */
    private fun printLine(text: String) {
        try {
            // 转换为GBK编码（中文打印机通常使用GBK）
            val bytes = text.toByteArray(charset("GBK"))
            outputStream?.write(bytes)
            outputStream?.write(LF)
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "打印文本失败: $text", e)
            throw e
        }
    }
    
    /**
     * 送纸指定行数
     */
    private fun feedLine(lines: Int) {
        try {
            for (i in 1..lines) {
                outputStream?.write(LF)
            }
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "送纸失败", e)
        }
    }
    
    /**
     * 切纸
     */
    private fun cutPaper() {
        try {
            // 全切纸命令
            writeBytes(byteArrayOf(GS, 0x56, 0x00))
        } catch (e: Exception) {
            Log.e(TAG, "切纸失败", e)
        }
    }
    
    /**
     * 打印二维码
     */
    private fun printQRCode(content: String) {
        try {
            // 设置QR码大小
            writeBytes(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x08))
            
            // 设置纠错等级
            writeBytes(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x30))
            
            // 设置QR码内容
            val contentBytes = content.toByteArray(charset("GBK"))
            val length = contentBytes.size + 3
            val pL = length % 256
            val pH = length / 256
            
            writeBytes(byteArrayOf(GS, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30))
            writeBytes(contentBytes)
            
            // 打印QR码
            writeBytes(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))
            
        } catch (e: Exception) {
            Log.e(TAG, "打印二维码失败", e)
        }
    }
    
    /**
     * 写入字节数组
     */
    private fun writeBytes(bytes: ByteArray) {
        try {
            outputStream?.write(bytes)
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e(TAG, "写入字节失败", e)
            throw e
        }
    }
    
    /**
     * 获取打印机状态
     */
    fun getPrinterStatus(): PrinterStatus {
        return if (isConnected()) {
            PrinterStatus.CONNECTED
        } else {
            PrinterStatus.DISCONNECTED
        }
    }
    
    enum class PrinterStatus {
        CONNECTED,
        DISCONNECTED,
        ERROR
    }
}

/**
 * 打印数据类
 */
data class PrintData(
    val orderNumber: String,
    val productName: String,
    val storageLocation: String,
    val quantity: String,
    val printTime: Long
)