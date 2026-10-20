package com.example.oneclicklock;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Toast;

/**
 * 设备管理员接收器
 * <p>
 * 用于响应设备管理员相关事件，如：
 * - 启用/禁用设备管理员
 * - 密码策略变更
 * - 锁屏事件等
 */
public class LockScreenAdminReceiver extends DeviceAdminReceiver {

    private static final String TAG = "OneClickLockAdmin";

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        Log.i(TAG, "Device admin enabled");
        Toast.makeText(context, "一键锁屏已激活", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
        Log.i(TAG, "Device admin disabled");
        Toast.makeText(context, "一键锁屏已停用", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLockTaskModeEntering(Context context, Intent intent, String pkg) {
        super.onLockTaskModeEntering(context, intent, pkg);
        Log.d(TAG, "Lock task mode entering: " + pkg);
    }

    @Override
    public void onLockTaskModeExiting(Context context, Intent intent) {
        super.onLockTaskModeExiting(context, intent);
        Log.d(TAG, "Lock task mode exiting");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        // 当用户尝试禁用设备管理员时，返回提示文本
        return "禁用后将无法使用一键锁屏功能";
    }

    // 密码相关回调在 Android 9+ (API 28+) 中签名变更为 UserHandle
    // 简单的锁屏应用不需要处理密码事件，注释掉避免编译错误
    /*
    @Override
    public void onPasswordChanged(Context context, Intent intent, UserHandle userHandle) {
        super.onPasswordChanged(context, intent, userHandle);
        Log.d(TAG, "Password changed for user: " + userHandle);
    }

    @Override
    public void onPasswordFailed(Context context, Intent intent, UserHandle userHandle) {
        super.onPasswordFailed(context, intent, userHandle);
        Log.d(TAG, "Password failed for user: " + userHandle);
    }

    @Override
    public void onPasswordSucceeded(Context context, Intent intent, UserHandle userHandle) {
        super.onPasswordSucceeded(context, intent, userHandle);
        Log.d(TAG, "Password succeeded for user: " + userHandle);
    }
    */
}