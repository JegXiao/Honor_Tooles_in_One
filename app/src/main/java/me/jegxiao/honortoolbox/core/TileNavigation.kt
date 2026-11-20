package me.jegxiao.honortoolbox.core

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService

/**
 * 从磁贴打开 Activity。
 * Android 14 起 startActivityAndCollapse(Intent) 被废弃，必须改用 PendingIntent；
 * 每个入口使用不同的 requestCode，避免 extras 被复用覆盖。
 */
fun TileService.startActivityFromTile(intent: Intent, requestCode: Int) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        startActivityAndCollapse(pendingIntent)
    } else {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(intent)
    }
}