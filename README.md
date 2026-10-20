# 一键锁屏应用 - OneClickLock

基于对原 APK (`一键锁屏_7.0.apk`) 反编译分析开发的现代化一键锁屏 Android 应用。

## 📱 原 APK 实现分析

### 核心代码 (`LockScreenActivity`)
```java
// 原 APK 核心逻辑 (反编译后)
PowerManager pm = (PowerManager) getSystemService("power");
boolean isOn = pm.isInteractive();  // 检查屏幕是否亮着

if (isOn) {
    // 反射调用隐藏 API
    Method goToSleep = PowerManager.class.getDeclaredMethod("goToSleep", Long.TYPE);
    goToSleep.setAccessible(true);
    goToSleep.invoke(pm, SystemClock.uptimeMillis());
}
finish();
System.exit(0);
```

### 关键发现
1. **使用反射调用 `PowerManager.goToSleep()`** - 这是隐藏 API
2. **需要 `android.permission.DEVICE_POWER` 权限** - 签名级权限，普通应用无法获取
3. **无设备管理员机制** - 直接尝试反射，失败则静默退出
4. **AndroidManifest** 声明了 `DEVICE_POWER` 权限和主 Activity

### 局限性
- 依赖隐藏 API，Android 版本升级可能失效
- 需要系统签名或特殊权限，普通应用无法在标准设备上工作
- 无优雅降级机制

---

## 🚀 新版实现方案

### 核心策略：三层级降级
```
┌─────────────────────────────────────────────────────────┐
│  1. DevicePolicyManager.lockNow()  ← 推荐，标准 API       │
│     └─ 需要：设备管理员权限 + force-lock 政策              │
├─────────────────────────────────────────────────────────┤
│  2. PowerManager.goToSleep() 反射  ← 备选，需特权          │
│     └─ 需要：DEVICE_POWER 签名权限 (系统应用可用)          │
├─────────────────────────────────────────────────────────┤
│  3. AccessibilityService GLOBAL_ACTION_LOCK_SCREEN  ← 备选  │
│     └─ 需要：用户开启无障碍服务，API 26+                   │
└─────────────────────────────────────────────────────────┘
```

### 实现文件
| 文件 | 说明 |
|------|------|
| `LockScreenActivity.java` | 主入口，实现三层级锁屏逻辑 + 权限引导 |
| `LockScreenAdminReceiver.java` | 设备管理员接收器 |
| `LockAccessibilityService.java` | 无障碍服务锁屏 (API 26+) |
| `device_admin_receiver.xml` | 声明 force-lock 政策 |
| `accessibility_service_config.xml` | 无障碍服务配置 |
| `AndroidManifest.xml` | 权限、组件声明 |

---

## 🛠 构建方法

### 前置要求
- JDK 17+
- Android SDK (compileSdk 34)
- Gradle 8.2+

### 使用 Android Studio
1. 打开 `OneClickLock` 文件夹
2. 等待 Gradle 同步
3. `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`

### 命令行构建 (已验证)
```bash
cd OneClickLock

# 使用项目自带的 gradle wrapper (需先下载 gradle-wrapper.jar)
./gradlew assembleDebug

# 或使用已解压的 gradle 分发版
D:\code\android-one-click-lock\OneClickLock\gradle\wrapper\temp\gradle-8.2.1\bin\gradle.bat assembleDebug
```

**输出 APK**: `app/build/outputs/apk/debug/app-debug.apk` (约 5.8 MB)

---

## 📋 安装与使用

### 1. 安装 APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. 激活权限 (按优先级引导)
首次打开应用会按以下优先级引导：

**方案一：设备管理员 (推荐，最稳定)**
> "请先激活设备管理员权限"
> 点击"激活" → 系统设置页面 → 勾选"一键锁屏设备管理员" → 确认激活

**方案二：无障碍服务 (备选，API 26+)**
> "建议开启无障碍服务作为备选方案"
> 设置 → 无障碍 → 已安装的服务 → 一键锁屏无障碍服务 → 开启

### 3. 使用
- 点击桌面图标 → 立即锁屏
- 可添加到桌面快捷方式、负一屏、侧边栏等
- 支持 Tasker、快捷指令等自动化工具调用 (`com.example.oneclicklock.LockScreenActivity`)

### 4. 卸载
必须先取消设备管理员：
```
设置 → 安全和隐私 → 设备管理员 → 一键锁屏设备管理员 → 停用
```
然后正常卸载应用。

---

## ⚙️ 技术细节

### 权限说明
| 权限 | 级别 | 用途 | 备注 |
|------|------|------|------|
| `DEVICE_POWER` | 签名 | 反射 goToSleep() | 普通应用不可得，保留兼容 |
| `BIND_DEVICE_ADMIN` | 系统 | 设备管理员接收器 | 必需，由系统授予 |
| `BIND_ACCESSIBILITY_SERVICE` | 系统 | 无障碍服务 | 用户手动开启 |

### 设备管理员政策
```xml
<!-- res/xml/device_admin_receiver.xml -->
<device-admin>
    <uses-policies>
        <force-lock />  <!-- 允许远程/代码锁屏 -->
    </uses-policies>
</device-admin>
```

### 无障碍服务配置
```xml
<!-- res/xml/accessibility_service_config.xml -->
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRequestFilterKeyEvents|flagRequestTouchExplorationMode"
    android:canRequestFilterKeyEvents="true"
    android:canRequestTouchExplorationMode="true"
    android:description="@string/accessibility_service_desc"
    android:settingsActivity="com.example.oneclicklock.LockScreenActivity" />
```

### 兼容性
| Android 版本 | DevicePolicyManager.lockNow() | Accessibility GLOBAL_ACTION_LOCK_SCREEN | 备注 |
|--------------|------------------------------|----------------------------------------|------|
| 5.0 (API 21) | ✅ 支持 | ❌ 不支持 | 需 force-lock 政策 |
| 7.0 (API 24) | ✅ 支持 | ❌ 不支持 | 标准化 |
| 8.0 (API 26) | ✅ 支持 | ✅ 支持 | 无障碍锁屏 API 引入 |
| 10.0 (API 29) | ✅ 支持 | ✅ 支持 | 增强安全 |
| 14+ (API 34) | ✅ 支持 | ✅ 支持 | 无额外限制 |

---

## 🔧 进阶配置

### 无障碍服务锁屏原理
使用 `AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN` (API 26+) 标准全局动作：
```java
// LockAccessibilityService.java
performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
```
优势：不需要设备管理员权限，普通应用可用
劣势：需要用户手动开启无障碍服务，仅 Android 8.0+ 支持

### 静默安装 (系统应用)
将 APK 放入 `/system/priv-app/` 并用平台密钥签名，可：
- 自动获得 `DEVICE_POWER` 权限
- 无需用户手动激活设备管理员
- 反射 `goToSleep()` 直接生效

---

## 📝 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0 | 2024 | 初始版本，基于原 APK 重写，实现三层级降级锁屏 |

---

## ⚠️ 注意事项

1. **必须激活设备管理员** - 否则 `lockNow()` 会抛出 SecurityException
2. **部分厂商 ROM 限制** - 小米、华为等可能需要额外授权"锁屏"权限
3. **Android 14+** - 对设备管理员有更严格限制，需测试兼容性
4. **不支持二次锁屏** - 已锁屏状态下点击图标无效果 (原 APK 同理)
5. **无障碍服务** - 部分厂商 ROM 可能限制或未正确实现 GLOBAL_ACTION_LOCK_SCREEN

---

## 📄 许可证

MIT License - 仅供学习研究使用