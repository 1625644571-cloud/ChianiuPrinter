package com.example.chianiuprinter

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChianiuAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "ChianiuAccessibility"
        private const val CHIANIU_PACKAGE_NAME = "com.meituan.chianiu" // 牵牛花App包名
        
        // 按钮文本（可能需要根据实际App调整）
        private const val CONFIRM_RECEIVE_BUTTON_TEXT = "确认收货"
        private const val CONFIRM_BUTTON_TEXT = "确认"
        private const val OK_BUTTON_TEXT = "确定"
        
        // 节点ID或文本（需要实际分析牵牛花App的界面）
        private val ORDER_NUMBER_KEYWORDS = listOf("收货单号", "单号", "订单号")
        private val PRODUCT_NAME_KEYWORDS = listOf("商品名称", "商品名", "品名")
        private val STORAGE_LOCATION_KEYWORDS = listOf("入库库位", "库位", "仓位")
        private val QUANTITY_KEYWORDS = listOf("数量", "件数", "个数")
        
        // 勾选框特征
        private val CHECKBOX_KEYWORDS = listOf("checkbox", "check", "勾选", "选择")
    }
    
    private var isMonitoring = false
    private var lastProcessedTime: Long = 0
    private val PROCESS_COOLDOWN = 2000L // 2秒冷却时间，防止重复处理
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "无障碍服务已连接")
        
        // 配置服务信息
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                        AccessibilityEvent.TYPE_VIEW_CLICKED or
                        AccessibilityEvent.TYPE_VIEW_FOCUSED
            
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                       AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            }
            
            notificationTimeout = 100
            packageNames = arrayOf(CHIANIU_PACKAGE_NAME)
        }
        
        this.serviceInfo = info
        isMonitoring = true
        
        // 发送广播通知服务已启动
        sendBroadcast(Intent(ACTION_ACCESSIBILITY_SERVICE_STARTED))
        
        Log.d(TAG, "开始监听牵牛花App")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isMonitoring) return
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastProcessedTime < PROCESS_COOLDOWN) {
            return // 冷却时间内不处理
        }
        
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    handleWindowStateChanged(event)
                }
                
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    handleWindowContentChanged(event)
                }
                
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    handleViewClicked(event)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理无障碍事件时出错", e)
        }
    }
    
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        val className = event.className?.toString()
        
        Log.d(TAG, "窗口状态变化: package=$packageName, class=$className")
        
        // 检查是否进入牵牛花App的收货单详情页面
        if (packageName == CHIANIU_PACKAGE_NAME) {
            // 这里可以根据className判断具体页面
            // 需要实际分析牵牛花App的页面结构
            Log.d(TAG, "进入牵牛花App页面")
        }
    }
    
    private fun handleWindowContentChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        if (packageName != CHIANIU_PACKAGE_NAME) return
        
        val rootNode = rootInActiveWindow ?: return
        
        // 查找确认收货按钮
        val confirmButton = findConfirmReceiveButton(rootNode)
        if (confirmButton != null) {
            Log.d(TAG, "找到确认收货按钮")
            // 可以在这里添加逻辑，比如高亮显示或记录日志
        }
        
        // 检查页面内容，提取商品信息
        val products = extractProductInfo(rootNode)
        if (products.isNotEmpty()) {
            Log.d(TAG, "找到 ${products.size} 个商品")
            // 可以在这里保存商品信息，供点击确认按钮时使用
            ProductDataCache.saveProducts(products)
        }
        
        rootNode.recycle()
    }
    
    private fun handleViewClicked(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        if (packageName != CHIANIU_PACKAGE_NAME) return
        
        val rootNode = rootInActiveWindow ?: return
        
        // 检查是否点击了确认收货按钮
        val clickedNode = findClickedNode(rootNode, event)
        if (clickedNode != null && isConfirmReceiveButton(clickedNode)) {
            Log.d(TAG, "检测到点击确认收货按钮")
            processConfirmReceive()
            lastProcessedTime = System.currentTimeMillis()
        }
        
        rootNode.recycle()
    }
    
    private fun findConfirmReceiveButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 方法1: 根据文本查找
        val nodesByText = rootNode.findAccessibilityNodeInfosByText(CONFIRM_RECEIVE_BUTTON_TEXT)
        if (nodesByText.isNotEmpty()) {
            return nodesByText[0]
        }
        
        // 方法2: 查找包含"确认"的按钮
        val confirmNodes = rootNode.findAccessibilityNodeInfosByText(CONFIRM_BUTTON_TEXT)
        for (node in confirmNodes) {
            if (node.isClickable && (node.className?.toString()?.contains("Button") == true || 
                node.className?.toString()?.contains("TextView") == true)) {
                return node
            }
        }
        
        // 方法3: 遍历所有节点查找按钮
        return findButtonByTraversal(rootNode, CONFIRM_RECEIVE_BUTTON_TEXT)
    }
    
    private fun findButtonByTraversal(
        node: AccessibilityNodeInfo, 
        targetText: String
    ): AccessibilityNodeInfo? {
        if (node.isClickable && node.text?.toString()?.contains(targetText) == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findButtonByTraversal(child, targetText)
                if (result != null) {
                    return result
                }
                child.recycle()
            }
        }
        
        return null
    }
    
    private fun findClickedNode(
        rootNode: AccessibilityNodeInfo, 
        event: AccessibilityEvent
    ): AccessibilityNodeInfo? {
        // 尝试根据事件坐标查找节点
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val x = event.x
            val y = event.y
            
            if (x >= 0 && y >= 0) {
                return rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            }
        }
        
        return null
    }
    
    private fun isConfirmReceiveButton(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        return text.contains(CONFIRM_RECEIVE_BUTTON_TEXT) || 
               text.contains(CONFIRM_BUTTON_TEXT) ||
               text.contains(OK_BUTTON_TEXT)
    }
    
    private fun extractProductInfo(rootNode: AccessibilityNodeInfo): List<ProductInfo> {
        val products = mutableListOf<ProductInfo>()
        
        // 查找所有勾选的商品
        val checkedProducts = findCheckedProducts(rootNode)
        
        for (productNode in checkedProducts) {
            val productInfo = extractProductFromNode(productNode)
            if (productInfo.isValid()) {
                products.add(productInfo)
            }
            productNode.recycle()
        }
        
        return products
    }
    
    private fun findCheckedProducts(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val checkedProducts = mutableListOf<AccessibilityNodeInfo>()
        
        // 查找所有勾选框
        val checkboxes = rootNode.findAccessibilityNodeInfosByViewId(".*checkbox.*")
        val checkboxesByText = rootNode.findAccessibilityNodeInfosByText(".*勾选.*")
        
        val allCheckboxes = mutableListOf<AccessibilityNodeInfo>()
        allCheckboxes.addAll(checkboxes)
        allCheckboxes.addAll(checkboxesByText)
        
        for (checkbox in allCheckboxes) {
            // 检查是否被勾选
            if (isCheckboxChecked(checkbox)) {
                // 找到对应的商品信息节点（通常是父节点或兄弟节点）
                val productNode = findProductNodeFromCheckbox(checkbox)
                if (productNode != null) {
                    checkedProducts.add(productNode)
                }
            }
            checkbox.recycle()
        }
        
        return checkedProducts
    }
    
    private fun isCheckboxChecked(checkbox: AccessibilityNodeInfo): Boolean {
        // 方法1: 检查是否被选中
        if (checkbox.isChecked) {
            return true
        }
        
        // 方法2: 检查内容描述
        val contentDescription = checkbox.contentDescription?.toString() ?: ""
        if (contentDescription.contains("已选择") || contentDescription.contains("选中")) {
            return true
        }
        
        // 方法3: 检查文本
        val text = checkbox.text?.toString() ?: ""
        if (text.contains("✓") || text.contains("√") || text.contains("已勾选")) {
            return true
        }
        
        return false
    }
    
    private fun findProductNodeFromCheckbox(checkbox: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // 尝试向上查找包含商品信息的父节点
        var parent = checkbox.parent
        while (parent != null) {
            // 检查父节点是否包含商品信息特征
            if (containsProductInfo(parent)) {
                return parent
            }
            val grandParent = parent.parent
            parent.recycle()
            parent = grandParent
        }
        
        return null
    }
    
    private fun containsProductInfo(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString() ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        
        // 检查是否包含商品相关信息
        return PRODUCT_NAME_KEYWORDS.any { text.contains(it) } ||
               STORAGE_LOCATION_KEYWORDS.any { text.contains(it) } ||
               QUANTITY_KEYWORDS.any { text.contains(it) }
    }
    
    private fun extractProductFromNode(productNode: AccessibilityNodeInfo): ProductInfo {
        val productInfo = ProductInfo()
        
        // 提取订单号（可能在整个页面的其他地方）
        val root = rootInActiveWindow
        if (root != null) {
            productInfo.orderNumber = extractFieldValue(root, ORDER_NUMBER_KEYWORDS)
            root.recycle()
        }
        
        // 提取商品信息
        productInfo.productName = extractFieldValue(productNode, PRODUCT_NAME_KEYWORDS)
        productInfo.storageLocation = extractFieldValue(productNode, STORAGE_LOCATION_KEYWORDS)
        productInfo.quantity = extractFieldValue(productNode, QUANTITY_KEYWORDS)
        
        // 设置当前时间
        productInfo.timestamp = System.currentTimeMillis()
        
        return productInfo
    }
    
    private fun extractFieldValue(
        node: AccessibilityNodeInfo, 
        keywords: List<String>
    ): String {
        // 遍历节点及其子节点，查找包含关键字的标签和对应的值
        return findFieldValueByTraversal(node, keywords)
    }
    
    private fun findFieldValueByTraversal(
        node: AccessibilityNodeInfo, 
        keywords: List<String>
    ): String {
        val text = node.text?.toString() ?: ""
        
        // 检查当前节点是否包含关键字
        for (keyword in keywords) {
            if (text.contains(keyword)) {
                // 找到关键字，尝试获取值（可能是兄弟节点或子节点）
                return extractValueFromNode(node, keyword)
            }
        }
        
        // 递归检查子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val value = findFieldValueByTraversal(child, keywords)
                if (value.isNotEmpty()) {
                    child.recycle()
                    return value
                }
                child.recycle()
            }
        }
        
        return ""
    }
    
    private fun extractValueFromNode(node: AccessibilityNodeInfo, keyword: String): String {
        // 方法1: 从文本中提取值（如"商品名称: XXX"）
        val text = node.text?.toString() ?: ""
        val parts = text.split(keyword)
        if (parts.size > 1) {
            return parts[1].trim().removePrefix(":").trim()
        }
        
        // 方法2: 查找兄弟节点
        val parent = node.parent ?: return ""
        
        for (i in 0 until parent.childCount) {
            val sibling = parent.getChild(i)
            if (sibling != null && sibling != node) {
                val siblingText = sibling.text?.toString() ?: ""
                if (siblingText.isNotEmpty() && !siblingText.contains(keyword)) {
                    parent.recycle()
                    sibling.recycle()
                    return siblingText.trim()
                }
                sibling.recycle()
            }
        }
        
        parent.recycle()
        return ""
    }
    
    private fun processConfirmReceive() {
        Log.d(TAG, "开始处理确认收货操作")
        
        // 从缓存获取商品信息
        val products = ProductDataCache.getProducts()
        if (products.isEmpty()) {
            Log.w(TAG, "没有找到商品信息")
            showToast("未找到商品信息")
            return
        }
        
        Log.d(TAG, "找到 ${products.size} 个待打印商品")
        
        // 检查打印机连接
        if (!BluetoothPrintManager.isConnected()) {
            Log.w(TAG, "打印机未连接")
            showToast("打印机未连接，请先连接打印机")
            return
        }
        
        // 逐个打印商品标签
        var successCount = 0
        for (product in products) {
            val printData = PrintData(
                orderNumber = product.orderNumber,
                productName = product.productName,
                storageLocation = product.storageLocation,
                quantity = product.quantity,
                printTime = System.currentTimeMillis()
            )
            
            val success = BluetoothPrintManager.printLabel(printData)
            if (success) {
                successCount++
                
                // 保存打印记录
                PrintHistoryManager.savePrintRecord(this, printData)
                
                Log.d(TAG, "打印成功: ${product.productName}")
            } else {
                Log.e(TAG, "打印失败: ${product.productName}")
            }
            
            // 添加短暂延迟，避免打印机处理不过来
            Thread.sleep(500)
        }
        
        // 显示结果
        val message = "打印完成: $successCount/${products.size} 成功"
        Log.d(TAG, message)
        showToast(message)
        
        // 清空缓存
        ProductDataCache.clear()
    }
    
    private fun showToast(message: String) {
        // 在主线程显示Toast
        mainHandler.post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
        isMonitoring = false
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "无障碍服务被销毁")
        isMonitoring = false
    }
    
    // 数据类
    data class ProductInfo(
        var orderNumber: String = "",
        var productName: String = "",
        var storageLocation: String = "",
        var quantity: String = "",
        var timestamp: Long = 0
    ) {
        fun isValid(): Boolean {
            return productName.isNotEmpty() && storageLocation.isNotEmpty() && quantity.isNotEmpty()
        }
    }
    
    // 缓存类
    object ProductDataCache {
        private var products: MutableList<ProductInfo> = mutableListOf()
        
        @Synchronized
        fun saveProducts(newProducts: List<ProductInfo>) {
            products.clear()
            products.addAll(newProducts)
        }
        
        @Synchronized
        fun getProducts(): List<ProductInfo> {
            return products.toList()
        }
        
        @Synchronized
        fun clear() {
            products.clear()
        }
    }
}