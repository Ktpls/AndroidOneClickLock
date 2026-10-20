package com.example.oneclicklock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * 无障碍服务锁屏实现
 * <p>
 * 原理：通过无障碍服务的 GLOBAL_ACTION_LOCK_SCREEN (API 26+) 全局动作锁屏
 * 优势：不需要设备管理员权限，普通应用可用
 * 劣势：需要用户手动开启无障碍服务，仅 Android 8.0+ 支持
 * <p>
 * 注意：GLOBAL_ACTION_LOCK_SCREEN 是 Android 8.0 (API 26) 引入的标准无障碍动作
 * 部分厂商 ROM 可能未正确实现或限制该动作
 */
public class LockAccessibilityService extends AccessibilityService {

    private static final String TAG = "OneClickLockAccessibility";
    public static final String ACTION_LOCK_SCREEN = "com.example.oneclicklock.ACTION_LOCK_SCREEN";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不处理事件，仅用于锁屏触发
    }

    @Override
    public void onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Accessibility service connected");

        // 配置服务信息
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                | AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
        // settingsActivityId 在 API 26+ 中废弃，配置在 XML meta-data 中
        setServiceInfo(info);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_LOCK_SCREEN.equals(intent.getAction())) {
            performLockScreen();
        }
        return START_NOT_STICKY;
    }

    /**
     * 执行锁屏操作
     */
    private void performLockScreen() {
        Log.d(TAG, "performLockScreen called");

        // 使用 GLOBAL_ACTION_LOCK_SCREEN (API 26+)
        // 这是无障碍服务锁屏的标准方式
        try {
            performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN);
            Log.d(TAG, "Locked via GLOBAL_ACTION_LOCK_SCREEN");
        } catch (Exception e) {
            Log.w(TAG, "GLOBAL_ACTION_LOCK_SCREEN failed: " + e.getMessage());
        }
    }
}