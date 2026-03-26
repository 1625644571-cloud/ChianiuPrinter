package com.example.chianiuprinter

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrintHistoryManager {
    
    companion object {
        private const TAG = "PrintHistoryManager"
        private const val PREFS_NAME = "print_history"
        private const val KEY_HISTORY = "print_history_list"
        private const val MAX_HISTORY_SIZE = 100 // 最大保存记录数
        
        private val gson = Gson()
    }
    
    /**
     * 保存打印记录
     */
    fun savePrintRecord(context: Context, printData: PrintData) {
        val history = getPrintHistory(context).toMutableList()
        
        // 创建历史记录
        val record = PrintRecord(
            id = System.currentTimeMillis(),
            orderNumber = printData.orderNumber,
            productName = printData.productName,
            storageLocation = printData.storageLocation,
            quantity = printData.quantity,
            printTime = printData.printTime,
            success = true
        )
        
        // 添加到列表开头
        history.add(0, record)
        
        // 限制记录数量
        if (history.size > MAX_HISTORY_SIZE) {
            history.subList(MAX_HISTORY_SIZE, history.size).clear()
        }
        
        // 保存到SharedPreferences
        saveHistoryToPrefs(context, history)
        
        // 发送广播通知有新记录
        sendNewRecordBroadcast(context, record)
    }
    
    /**
     * 获取打印历史
     */
    fun getPrintHistory(context: Context): List<PrintRecord> {
        return loadHistoryFromPrefs(context)
    }
    
    /**
     * 获取最后一条打印记录
     */
    fun getLastPrint(context: Context): PrintRecord? {
        val history = getPrintHistory(context)
        return history.firstOrNull()
    }
    
    /**
     * 根据ID获取打印记录
     */
    fun getPrintRecordById(context: Context, id: Long): PrintRecord? {
        val history = getPrintHistory(context)
        return history.find { it.id == id }
    }
    
    /**
     * 根据订单号筛选记录
     */
    fun getRecordsByOrderNumber(context: Context, orderNumber: String): List<PrintRecord> {
        val history = getPrintHistory(context)
        return history.filter { it.orderNumber.contains(orderNumber, ignoreCase = true) }
    }
    
    /**
     * 根据商品名称筛选记录
     */
    fun getRecordsByProductName(context: Context, productName: String): List<PrintRecord> {
        val history = getPrintHistory(context)
        return history.filter { it.productName.contains(productName, ignoreCase = true) }
    }
    
    /**
     * 根据时间范围筛选记录
     */
    fun getRecordsByTimeRange(
        context: Context, 
        startTime: Long, 
        endTime: Long
    ): List<PrintRecord> {
        val history = getPrintHistory(context)
        return history.filter { it.printTime in startTime..endTime }
    }
    
    /**
     * 删除打印记录
     */
    fun deletePrintRecord(context: Context, id: Long): Boolean {
        val history = getPrintHistory(context).toMutableList()
        val initialSize = history.size
        
        history.removeAll { it.id == id }
        
        if (history.size < initialSize) {
            saveHistoryToPrefs(context, history)
            return true
        }
        
        return false
    }
    
    /**
     * 清空打印历史
     */
    fun clearPrintHistory(context: Context) {
        saveHistoryToPrefs(context, emptyList())
    }
    
    /**
     * 获取统计信息
     */
    fun getStatistics(context: Context): PrintStatistics {
        val history = getPrintHistory(context)
        
        return PrintStatistics(
            totalPrints = history.size,
            successfulPrints = history.count { it.success },
            failedPrints = history.count { !it.success },
            todayPrints = history.count { isToday(it.printTime) },
            lastPrintTime = history.firstOrNull()?.printTime ?: 0
        )
    }
    
    /**
     * 导出打印历史为CSV格式
     */
    fun exportToCsv(context: Context): String {
        val history = getPrintHistory(context)
        val csv = StringBuilder()
        
        // 添加标题行
        csv.append("ID,订单号,商品名称,入库库位,数量,打印时间,状态\n")
        
        // 添加数据行
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        for (record in history) {
            csv.append("${record.id},")
            csv.append("\"${record.orderNumber}\",")
            csv.append("\"${record.productName}\",")
            csv.append("\"${record.storageLocation}\",")
            csv.append("\"${record.quantity}\",")
            csv.append("\"${dateFormat.format(Date(record.printTime))}\",")
            csv.append(if (record.success) "成功" else "失败")
            csv.append("\n")
        }
        
        return csv.toString()
    }
    
    /**
     * 导出打印历史为JSON格式
     */
    fun exportToJson(context: Context): String {
        val history = getPrintHistory(context)
        return gson.toJson(history)
    }
    
    /**
     * 从SharedPreferences加载历史记录
     */
    private fun loadHistoryFromPrefs(context: Context): List<PrintRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        
        return try {
            val type = object : TypeToken<List<PrintRecord>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 保存历史记录到SharedPreferences
     */
    private fun saveHistoryToPrefs(context: Context, history: List<PrintRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(history)
        
        prefs.edit()
            .putString(KEY_HISTORY, json)
            .apply()
    }
    
    /**
     * 发送新记录广播
     */
    private fun sendNewRecordBroadcast(context: Context, record: PrintRecord) {
        // 这里可以发送广播通知其他组件
        // 例如更新UI或触发其他操作
    }
    
    /**
     * 检查是否是今天
     */
    private fun isToday(timestamp: Long): Boolean {
        val today = Date()
        val recordDate = Date(timestamp)
        
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
        return dateFormat.format(today) == dateFormat.format(recordDate)
    }
}

/**
 * 打印记录数据类
 */
data class PrintRecord(
    val id: Long,
    val orderNumber: String,
    val productName: String,
    val storageLocation: String,
    val quantity: String,
    val printTime: Long,
    val success: Boolean
)

/**
 * 打印统计信息
 */
data class PrintStatistics(
    val totalPrints: Int,
    val successfulPrints: Int,
    val failedPrints: Int,
    val todayPrints: Int,
    val lastPrintTime: Long
)

/**
 * 日期工具类
 */
object DateUtils {
    
    fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return dateFormat.format(Date(timestamp))
    }
    
    fun formatDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        return dateFormat.format(Date(timestamp))
    }
    
    fun formatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
        return dateFormat.format(Date(timestamp))
    }
    
    fun getTodayStartTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun getTodayEndTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}