package com.example.oneclicklock;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

/**
 * 快捷设置磁贴服务 - 一键锁屏
 * <p>
 * 允许用户从下拉通知栏的快捷设置面板（和蓝牙、热点、定位等在一起的地方）
 * 直接点击锁屏，无需打开应用。
 * <p>
 * 使用方法：
 * 1. 下拉通知栏 → 再次下拉展开快捷设置
 * 2. 点击右下角"编辑"或铅笔图标
 * 3. 找到"一键锁屏"磁贴，拖拽到上方激活区域
 * 4. 点击磁贴即可锁屏
 * <p>
 * 需要权限：android.permission.BIND_QUICK_SETTINGS_TILE (系统自动授予)
 * 目标 API：Android 7.0 (API 24)+
 */
public class LockScreenTileService extends TileService {

    private static final String TAG = "OneClickLockTile";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "TileService created");
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Log.d(TAG, "Tile added to Quick Settings");
        // 磁贴被添加到快捷设置面板时调用
        updateTileState(false);
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
        Log.d(TAG, "Tile removed from Quick Settings");
        // 磁贴从快捷设置面板移除时调用
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.d(TAG, "Tile started listening");
        // 磁贴可见时调用（如下拉通知栏）
        updateTileState(false);
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Log.d(TAG, "Tile stopped listening");
        // 磁贴不可见时调用
    }

    @Override
    public void onClick() {
        super.onClick();
        Log.d(TAG, "Tile clicked - locking screen");
        // 用户点击磁贴时调用 - 执行锁屏
        lockScreen();
    }

    /**
     * 更新磁贴状态（图标、标签、状态）
     */
    private void updateTileState(boolean isLocked) {
        Tile tile = getQsTile();
        if (tile == null) return;

        // 设置磁贴状态
        // Tile.STATE_ACTIVE = 亮起/激活状态
        // Tile.STATE_INACTIVE = 正常/非激活状态
        // Tile.STATE_UNAVAILABLE = 不可用（灰色）
        tile.setState(Tile.STATE_ACTIVE);

        // 设置图标（使用应用图标）
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_launcher));

        // 设置标签
        tile.setLabel(getString(R.string.tile_label));

        // 设置副标题（可选）
        tile.setSubtitle(getString(R.string.tile_subtitle));

        // 设置内容描述（无障碍）
        tile.setContentDescription(getString(R.string.tile_content_desc));

        // 更新磁贴
        tile.updateTile();
    }

    /**
     * 执行锁屏操作
     * 复用 LockScreenActivity 的锁屏逻辑
     */
    private void lockScreen() {
        // 启动 LockScreenActivity 执行锁屏
        // 使用新任务标志，因为从 TileService 启动
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // 标记这是从 Tile 触发的，避免显示 Toast 等 UI 干扰
        intent.putExtra("from_tile", true);
        startActivity(intent);

        // 可选：锁屏后将磁贴设为非激活状态
        // 注意：锁屏后屏幕关闭，用户解锁后再看到磁贴时会重新 onStartListening
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }
}