package com.example.chianiuprinter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrintHistoryActivity : AppCompatActivity() {
    
    // UI组件
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var tvStatistics: TextView
    
    // 适配器
    private lateinit var adapter: PrintHistoryAdapter
    
    // 数据
    private var printHistory: List<PrintRecord> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print_history)
        
        // 初始化UI组件
        initViews()
        
        // 设置RecyclerView
        setupRecyclerView()
        
        // 加载数据
        loadPrintHistory()
        
        // 更新统计信息
        updateStatistics()
    }
    
    override fun onResume() {
        super.onResume()
        // 刷新数据
        loadPrintHistory()
        updateStatistics()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        tvEmpty = findViewById(R.id.tv_empty)
        tvStatistics = findViewById(R.id.tv_statistics)
        
        // 返回按钮
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }
        
        // 刷新按钮
        findViewById<View>(R.id.btn_refresh).setOnClickListener {
            loadPrintHistory()
            updateStatistics()
        }
        
        // 筛选按钮
        findViewById<View>(R.id.btn_filter).setOnClickListener {
            showFilterDialog()
        }
        
        // 排序按钮
        findViewById<View>(R.id.btn_sort).setOnClickListener {
            showSortDialog()
        }
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PrintHistoryAdapter()
        recyclerView.adapter = adapter
        
        // 添加分割线
        recyclerView.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                this,
                LinearLayoutManager.VERTICAL
            )
        )
    }
    
    private fun loadPrintHistory() {
        printHistory = PrintHistoryManager.getPrintHistory(this)
        adapter.submitList(printHistory)
        
        // 显示/隐藏空状态
        if (printHistory.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun updateStatistics() {
        val stats = PrintHistoryManager.getStatistics(this)
        
        val statisticsText = """
            总计: ${stats.totalPrints} 条
            成功: ${stats.successfulPrints} 条
            失败: ${stats.failedPrints} 条
            今日: ${stats.todayPrints} 条
        """.trimIndent()
        
        tvStatistics.text = statisticsText
    }
    
    private fun showFilterDialog() {
        val filterOptions = arrayOf(
            "全部记录",
            "仅今日",
            "仅成功",
            "仅失败",
            "按订单号筛选",
            "按商品名称筛选"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("筛选记录")
            .setItems(filterOptions) { dialog, which ->
                when (which) {
                    0 -> filterAll()
                    1 -> filterToday()
                    2 -> filterSuccessful()
                    3 -> filterFailed()
                    4 -> showOrderNumberFilterDialog()
                    5 -> showProductNameFilterDialog()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "按时间倒序（最新在前）",
            "按时间正序（最早在前）",
            "按订单号排序",
            "按商品名称排序"
        )
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setItems(sortOptions) { dialog, which ->
                when (which) {
                    0 -> sortByTimeDesc()
                    1 -> sortByTimeAsc()
                    2 -> sortByOrderNumber()
                    3 -> sortByProductName()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun filterAll() {
        loadPrintHistory()
        showToast("显示全部记录")
    }
    
    private fun filterToday() {
        val todayStart = DateUtils.getTodayStartTime()
        val todayEnd = DateUtils.getTodayEndTime()
        
        val filtered = PrintHistoryManager.getRecordsByTimeRange(this, todayStart, todayEnd)
        adapter.submitList(filtered)
        
        showToast("显示今日记录: ${filtered.size} 条")
    }
    
    private fun filterSuccessful() {
        val filtered = printHistory.filter { it.success }
        adapter.submitList(filtered)
        
        showToast("显示成功记录: ${filtered.size} 条")
    }
    
    private fun filterFailed() {
        val filtered = printHistory.filter { !it.success }
        adapter.submitList(filtered)
        
        showToast("显示失败记录: ${filtered.size} 条")
    }
    
    private fun showOrderNumberFilterDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("按订单号筛选")
            .setMessage("请输入订单号（支持模糊匹配）：")
            .setView(android.widget.EditText(this).apply {
                hint = "例如: SH-20250326"
            })
            .setPositiveButton("筛选") { dialog, which ->
                val editText = (dialog as androidx.appcompat.app.AlertDialog).findViewById<android.widget.EditText>(android.R.id.edit)
                val orderNumber = editText?.text?.toString() ?: ""
                
                if (orderNumber.isNotEmpty()) {
                    val filtered = PrintHistoryManager.getRecordsByOrderNumber(this, orderNumber)
                    adapter.submitList(filtered)
                    showToast("找到 ${filtered.size} 条记录")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showProductNameFilterDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("按商品名称筛选")
            .setMessage("请输入商品名称（支持模糊匹配）：")
            .setView(android.widget.EditText(this).apply {
                hint = "例如: 喷雾"
            })
            .setPositiveButton("筛选") { dialog, which ->
                val editText = (dialog as androidx.appcompat.app.AlertDialog).findViewById<android.widget.EditText>(android.R.id.edit)
                val productName = editText?.text?.toString() ?: ""
                
                if (productName.isNotEmpty()) {
                    val filtered = PrintHistoryManager.getRecordsByProductName(this, productName)
                    adapter.submitList(filtered)
                    showToast("找到 ${filtered.size} 条记录")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun sortByTimeDesc() {
        val sorted = printHistory.sortedByDescending { it.printTime }
        adapter.submitList(sorted)
        showToast("按时间倒序排序")
    }
    
    private fun sortByTimeAsc() {
        val sorted = printHistory.sortedBy { it.printTime }
        adapter.submitList(sorted)
        showToast("按时间正序排序")
    }
    
    private fun sortByOrderNumber() {
        val sorted = printHistory.sortedBy { it.orderNumber }
        adapter.submitList(sorted)
        showToast("按订单号排序")
    }
    
    private fun sortByProductName() {
        val sorted = printHistory.sortedBy { it.productName }
        adapter.submitList(sorted)
        showToast("按商品名称排序")
    }
    
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 打印历史适配器
     */
    inner class PrintHistoryAdapter : RecyclerView.Adapter<PrintHistoryAdapter.ViewHolder>() {
        
        private var items: List<PrintRecord> = emptyList()
        
        fun submitList(newItems: List<PrintRecord>) {
            items = newItems
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_print_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val record = items[position]
            holder.bind(record)
            
            // 点击事件
            holder.itemView.setOnClickListener {
                showRecordDetails(record)
            }
            
            // 长按事件
            holder.itemView.setOnLongClickListener {
                showRecordActions(record)
                true
            }
        }
        
        override fun getItemCount(): Int = items.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvOrderNumber: TextView = itemView.findViewById(R.id.tv_order_number)
            private val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
            private val tvStorageLocation: TextView = itemView.findViewById(R.id.tv_storage_location)
            private val tvQuantity: TextView = itemView.findViewById(R.id.tv_quantity)
            private val tvPrintTime: TextView = itemView.findViewById(R.id.tv_print_time)
            private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
            
            fun bind(record: PrintRecord) {
                tvOrderNumber.text = "单号: ${record.orderNumber}"
                tvProductName.text = "商品: ${record.productName}"
                tvStorageLocation.text = "库位: ${record.storageLocation}"
                tvQuantity.text = "数量: ${record.quantity}"
                
                val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                tvPrintTime.text = dateFormat.format(Date(record.printTime))
                
                tvStatus.text = if (record.success) "成功" else "失败"
                tvStatus.setTextColor(
                    if (record.success) {
                        getColor(android.R.color.holo_green_dark)
                    } else {
                        getColor(android.R.color.holo_red_dark)
                    }
                )
            }
        }
    }
    
    private fun showRecordDetails(record: PrintRecord) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val details = """
            订单号: ${record.orderNumber}
            商品名称: ${record.productName}
            入库库位: ${record.storageLocation}
            数量: ${record.quantity}
            打印时间: ${dateFormat.format(Date(record.printTime))}
            状态: ${if (record.success) "成功" else "失败"}
            记录ID: ${record.id}
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("记录详情")
            .setMessage(details)
            .setPositiveButton("关闭", null)
            .show()
    }
    
    private fun showRecordActions(record: PrintRecord) {
        val actions = arrayOf("重新打印", "删除记录", "分享记录", "取消")
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("操作")
            .setItems(actions) { dialog, which ->
                when (which) {
                    0 -> reprintRecord(record)
                    1 -> deleteRecord(record)
                    2 -> shareRecord(record)
                }
            }
            .show()
    }
    
    private fun reprintRecord(record: PrintRecord) {
        if (!BluetoothPrintManager.isConnected()) {
            showToast("打印机未连接")
            return
        }
        
        val printData = PrintData(
            orderNumber = record.orderNumber,
            productName = record.productName,
            storageLocation = record.storageLocation,
            quantity = record.quantity,
            printTime = System.currentTimeMillis()
        )
        
        Thread {
            val success = BluetoothPrintManager.printLabel(printData)
            
            runOnUiThread {
                if (success) {
                    showToast("重新打印成功")
                    
                    // 保存新的打印记录
                    PrintHistoryManager.savePrintRecord(this, printData)
                    
                    // 刷新列表
                    loadPrintHistory()
                    updateStatistics()
                } else {
                    showToast("重新打印失败")
                }
            }
        }.start()
    }
    
    private fun deleteRecord(record: PrintRecord) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定要删除这条打印记录吗？")
            .setPositiveButton("删除") { dialog, which ->
                val success = PrintHistoryManager.deletePrintRecord(this, record.id)
                if (success) {
                    showToast("记录已删除")
                    loadPrintHistory()
                    updateStatistics()
                } else {
                    showToast("删除失败")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun shareRecord(record: PrintRecord) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        val shareText = """
            牵牛花打印记录
            订单号: ${record.orderNumber}
            商品名称: ${record.productName}
            入库库位: ${record.storageLocation}
            数量: ${record.quantity}
            打印时间: ${dateFormat.format(Date(record.printTime))}
            状态: ${if (record.success) "成功" else "失败"}
        """.trimIndent()
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "牵牛花打印记录")
        }
        
        startActivity(Intent.createChooser(intent, "分享打印记录"))
    }
}