package com.example.oneclicklock;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

/**
 * 一键锁屏主 Activity
 * <p>
 * 实现原理（三层级降级）：
 * 1. 优先使用 DevicePolicyManager.lockNow() (需要设备管理员权限，标准 API)
 * 2. 回退到 PowerManager.goToSleep() 反射调用 (需要 DEVICE_POWER 权限或系统签名)
 * 3. 最后尝试无障碍服务 GLOBAL_ACTION_LOCK_SCREEN (需要无障碍权限，API 26+)
 * <p>
 * 支持从快捷设置磁贴触发：intent.putExtra("from_tile", true)
 * 从磁贴触发时静默尝试锁屏，不显示 Toast 或引导界面
 */
public class LockScreenActivity extends Activity {

    private static final String TAG = "OneClickLock";
    private static final int REQUEST_DEVICE_ADMIN = 1001;
    private static final int REQUEST_ACCESSIBILITY = 1002;
    private static final String EXTRA_FROM_TILE = "from_tile";

    private DevicePolicyManager devicePolicyManager;
    private ComponentName adminComponent;
    private boolean fromTile = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 检查是否从快捷设置磁贴启动
        fromTile = getIntent().getBooleanExtra(EXTRA_FROM_TILE, false);

        // 初始化设备策略管理器
        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, LockScreenAdminReceiver.class);

        // 尝试锁屏
        boolean locked = tryLockScreen();

        if (!locked) {
            if (fromTile) {
                // 从磁贴触发且锁屏失败：静默结束，不显示 Toast 或引导
                Log.w(TAG, "Lock failed from tile, silently finishing");
                finish();
            } else {
                // 正常启动：按优先级引导用户开启对应权限
                handleLockFailure();
            }
        } else {
            finish();
        }
    }

    /**
     * 处理锁屏失败 - 正常启动时的引导逻辑
     */
    private void handleLockFailure() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            // 优先引导设备管理员 (最稳定)
            Toast.makeText(this, "请先激活设备管理员权限", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "一键锁屏需要设备管理员权限来锁定屏幕 (推荐方式，最稳定)");
            startActivityForResult(intent, REQUEST_DEVICE_ADMIN);
        } else if (!isAccessibilityEnabled()) {
            // 次选引导无障碍服务
            Toast.makeText(this, "建议开启无障碍服务作为备选方案", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, REQUEST_ACCESSIBILITY);
        } else {
            Toast.makeText(this, "锁屏失败，请检查权限", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DEVICE_ADMIN) {
            if (resultCode == RESULT_OK) {
                // 用户激活了设备管理员，再次尝试锁屏
                if (tryLockScreen()) {
                    finish();
                } else {
                    // 设备管理员激活但锁屏失败，引导开启无障碍
                    if (!isAccessibilityEnabled()) {
                        Toast.makeText(this, "建议开启无障碍服务作为备选", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        startActivityForResult(intent, REQUEST_ACCESSIBILITY);
                    } else {
                        finish();
                    }
                }
            } else {
                Toast.makeText(this, "未授权设备管理员，尝试无障碍服务方案", Toast.LENGTH_LONG).show();
                if (!isAccessibilityEnabled()) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivityForResult(intent, REQUEST_ACCESSIBILITY);
                } else {
                    tryLockScreen();
                    finish();
                }
            }
        } else if (requestCode == REQUEST_ACCESSIBILITY) {
            // 从无障碍设置返回，再次尝试
            if (tryLockScreen()) {
                finish();
            } else {
                Toast.makeText(this, "请确保已开启\"一键锁屏无障碍服务\"", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * 尝试多种方式锁屏 (按优先级)
     * @return 是否成功锁屏
     */
    private boolean tryLockScreen() {
        // 方式 1: DevicePolicyManager.lockNow() - 标准 API，需要设备管理员
        if (tryLockWithDevicePolicyManager()) {
            Log.d(TAG, "Locked via DevicePolicyManager.lockNow()");
            return true;
        }

        // 方式 2: PowerManager.goToSleep() 反射 - 需要 DEVICE_POWER 权限
        if (tryLockWithGoToSleep()) {
            Log.d(TAG, "Locked via PowerManager.goToSleep() reflection");
            return true;
        }

        // 方式 3: 无障碍服务 GLOBAL_ACTION_LOCK_SCREEN (API 26+)
        if (tryLockWithAccessibilityService()) {
            Log.d(TAG, "Locked via AccessibilityService");
            return true;
        }

        return false;
    }

    /**
     * 使用 DevicePolicyManager.lockNow() 锁屏
     * 这是 Android 官方推荐的方式，需要设备管理员权限
     */
    private boolean tryLockWithDevicePolicyManager() {
        try {
            if (devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent)) {
                // Android 14+ 可以直接调用，旧版本需要 force-lock 政策
                devicePolicyManager.lockNow();
                return true;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "DevicePolicyManager.lockNow() security exception: " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "DevicePolicyManager.lockNow() failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * 使用反射调用 PowerManager.goToSleep()
     * 注意：Android 6.0+ 需要 DEVICE_POWER 权限 (签名级)
     * 普通应用通常无法获取，除非是系统应用或有特殊签名
     */
    private boolean tryLockWithGoToSleep() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm == null) return false;

            // 检查屏幕是否已经关闭
            boolean isInteractive;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                isInteractive = pm.isInteractive();
            } else {
                isInteractive = pm.isScreenOn();
            }

            if (!isInteractive) {
                Log.d(TAG, "Screen already off");
                return true;
            }

            // 反射调用 goToSleep(long)
            Method goToSleep = PowerManager.class.getDeclaredMethod("goToSleep", long.class);
            goToSleep.setAccessible(true);
            goToSleep.invoke(pm, android.os.SystemClock.uptimeMillis());
            return true;

        } catch (NoSuchMethodException e) {
            Log.w(TAG, "goToSleep method not found: " + e.getMessage());
        } catch (SecurityException e) {
            Log.w(TAG, "goToSleep security exception (need DEVICE_POWER permission): " + e.getMessage());
        } catch (Exception e) {
            Log.w(TAG, "goToSleep reflection failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * 使用无障碍服务锁屏 (API 26+)
     * 通过 GLOBAL_ACTION_LOCK_SCREEN 全局动作
     */
    private boolean tryLockWithAccessibilityService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.d(TAG, "Accessibility GLOBAL_ACTION_LOCK_SCREEN requires API 26+");
            return false;
        }

        if (!isAccessibilityEnabled()) {
            Log.d(TAG, "Accessibility service not enabled");
            return false;
        }

        try {
            // 启动无障碍服务并请求锁屏
            Intent intent = new Intent(this, LockAccessibilityService.class);
            intent.setAction("com.example.oneclicklock.ACTION_LOCK_SCREEN");
            startService(intent);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Accessibility service lock failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * 检查无障碍服务是否已启用
     */
    private boolean isAccessibilityEnabled() {
        String serviceId = getPackageName() + "/" + LockAccessibilityService.class.getCanonicalName();
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled == 1) {
                String enabledServices = Settings.Secure.getString(
                        getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (enabledServices != null && enabledServices.contains(serviceId)) {
                    return true;
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            Log.w(TAG, "Error checking accessibility: " + e.getMessage());
        }
        return false;
    }
}