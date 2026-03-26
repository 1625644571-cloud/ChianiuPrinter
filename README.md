# 牵牛花打印助手

一个Android应用，用于监听牵牛花App（美团旗下仓储管理App）的收货确认操作，自动连接德佟P1打印机并打印商品标签。

## 功能特性

- ✅ 监听牵牛花App的收货确认操作（通过无障碍服务）
- ✅ 自动提取商品信息（收货单号、商品名称、入库库位、数量）
- ✅ 蓝牙连接德佟P1热敏打印机
- ✅ 自动打印商品标签
- ✅ 打印历史记录管理
- ✅ 支持开机自启
- ✅ 支持自动重连打印机
- ✅ 友好的用户界面

## 系统要求

- Android 8.0 (API 26) 或更高版本
- 支持蓝牙4.0或更高版本
- 德佟P1热敏打印机（58mm纸宽）

## 安装说明

### 方法1：直接安装APK（推荐）

1. 下载APK文件到Android设备
2. 在设备上打开APK文件
3. 如果提示"禁止安装来自未知来源的应用"，请先开启"未知来源"设置
4. 按照提示完成安装

### 方法2：通过Android Studio构建

1. 克隆或下载本项目
2. 使用Android Studio打开项目
3. 连接Android设备或启动模拟器
4. 点击运行按钮（▶️）构建并安装应用

### 方法3：使用Gradle命令行构建

```bash
# 进入项目目录
cd ChianiuPrinterApp

# 构建APK
./gradlew assembleDebug

# 构建发布版APK
./gradlew assembleRelease

# 安装到已连接的设备
./gradlew installDebug
```

构建的APK文件位于：
- 调试版：`app/build/outputs/apk/debug/app-debug.apk`
- 发布版：`app/build/outputs/apk/release/app-release.apk`

## 使用说明

### 首次使用设置

1. **开启无障碍服务**
   - 打开应用后，点击"开启无障碍服务"按钮
   - 在系统设置中找到"牵牛花打印助手"
   - 开启无障碍服务开关

2. **连接打印机**
   - 确保德佟P1打印机已开机并处于配对模式
   - 在Android系统设置中配对打印机
   - 返回应用，点击"连接打印机"按钮
   - 选择已配对的德佟P1打印机

3. **启动服务**
   - 点击"启动服务"按钮
   - 应用将在后台运行并监听牵牛花App

### 使用流程

1. 打开牵牛花App，进入收货单详情页面
2. 勾选需要打印标签的商品
3. 点击"确认收货"按钮
4. 应用将自动检测操作并打印商品标签

### 标签格式示例

```
收货标签
==========

收货单号: SH-20250326-587715
商品名称: JVR杰威尔强塑定型喷雾
入库库位: HZ-1-2
数量: 3瓶

打印时间: 2025-03-26 14:30:25
----------------
```

## 权限说明

应用需要以下权限：

- **蓝牙权限**：连接和通信打印机
- **位置权限**：搜索蓝牙设备（Android 6.0+要求）
- **无障碍服务权限**：监听牵牛花App界面操作
- **前台服务权限**：在后台持续运行
- **开机启动权限**：实现开机自启功能

## 技术架构

### 核心组件

1. **无障碍服务** (`ChianiuAccessibilityService`)
   - 监听牵牛花App界面变化
   - 检测"确认收货"按钮点击
   - 提取商品信息

2. **蓝牙打印管理器** (`BluetoothPrintManager`)
   - 管理蓝牙连接
   - 实现ESC/POS打印协议
   - 处理打印任务队列

3. **前台服务** (`ForegroundService`)
   - 保持应用在后台运行
   - 显示常驻通知
   - 管理服务生命周期

4. **打印历史管理器** (`PrintHistoryManager`)
   - 保存打印记录
   - 支持查询和筛选
   - 导出历史数据

### 关键技术

- **Kotlin**：主要开发语言
- **Android Jetpack**：现代Android开发组件
- **Material Design**：遵循Material Design设计规范
- **ESC/POS协议**：热敏打印机通信标准
- **SharedPreferences**：轻量级数据存储

## 配置说明

### 牵牛花App包名

默认配置的牵牛花App包名为：`com.meituan.chianiu`

如果实际包名不同，需要修改以下文件：
- `AndroidManifest.xml`：无障碍服务配置
- `ChianiuAccessibilityService.kt`：包名常量

### 打印机配置

应用默认支持德佟P1打印机，使用标准ESC/POS协议。

如果需要支持其他打印机，可能需要调整：
- 蓝牙UUID
- ESC/POS命令序列
- 打印格式设置

## 故障排除

### 常见问题

1. **无法连接打印机**
   - 检查打印机是否开机
   - 检查打印机是否已配对
   - 检查蓝牙权限是否已授予
   - 重启应用和蓝牙

2. **无法监听牵牛花App**
   - 检查无障碍服务是否已开启
   - 检查牵牛花App包名是否正确
   - 重启牵牛花App

3. **打印标签乱码**
   - 检查打印机字符编码设置
   - 确保使用GBK编码
   - 更新打印机固件

4. **应用在后台被关闭**
   - 检查电池优化设置
   - 将应用加入白名单
   - 开启自启动权限

### 调试模式

应用支持调试日志，可以通过以下方式查看：
- Android Studio Logcat
- `adb logcat -s ChianiuAccessibility`
- `adb logcat -s BluetoothPrint`

## 开发说明

### 项目结构

```
ChianiuPrinterApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/chianiuprinter/
│   │   │   ├── MainActivity.kt              # 主界面
│   │   │   ├── ChianiuAccessibilityService.kt # 无障碍服务
│   │   │   ├── BluetoothPrintManager.kt     # 蓝牙打印管理
│   │   │   ├── ForegroundService.kt         # 前台服务
│   │   │   ├── ServiceManager.kt            # 服务管理
│   │   │   ├── PrintHistoryManager.kt       # 打印历史管理
│   │   │   ├── SettingsActivity.kt          # 设置界面
│   │   │   ├── PrintHistoryActivity.kt      # 打印历史界面
│   │   │   └── BootReceiver.kt              # 开机启动接收器
│   │   ├── res/                             # 资源文件
│   │   └── AndroidManifest.xml              # 应用清单
│   └── build.gradle                         # 构建配置
├── build.gradle                             # 项目构建配置
└── README.md                                # 说明文档
```

### 依赖库

- `androidx.core:core-ktx`：AndroidX核心库
- `com.google.android.material:material`：Material Design组件
- `androidx.constraintlayout:constraintlayout`：约束布局
- `androidx.recyclerview:recyclerview`：列表视图
- `com.google.code.gson:gson`：JSON序列化
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`：协程支持

## 版本历史

### v1.0.0 (2025-03-26)
- 初始版本发布
- 基础功能实现
- 支持德佟P1打印机
- 完整的打印历史管理

## 免责声明

本应用仅用于辅助仓储管理工作，不存储任何用户敏感信息。使用前请确保已获得相关授权。

## 技术支持

如有问题或建议，请联系开发者。

---

© 2025 牵牛花打印助手