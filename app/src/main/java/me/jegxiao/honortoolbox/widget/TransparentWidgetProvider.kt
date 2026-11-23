package me.jegxiao.honortoolbox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import me.jegxiao.honortoolbox.R

/**
 * 近乎透明的桌面占位组件。
 *
 * 用途：拉伸后占住桌面网格，让壁纸保持干净，也可以把新安装的 App 挤到后面的页面。
 *
 * 点按不会跳转、不会打开本应用，也不会有功能响应。注意：部分桌面（实测荣耀 MagicOS）
 * 会对“没有点击事件”的组件默认执行打开所属 App 的操作，所以这里显式注册一个空的
 * Broadcast PendingIntent 来吃掉点击事件，但 onReceive 里直接返回，不产生任何界面。
 * 布局里保留一圈极淡的双色轮廓，避免完全透明后无法在桌面上找到它。
 */
class TransparentWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_transparent)
        views.setOnClickPendingIntent(R.id.widget_transparent_root, noopIntent(context))
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 只消费点击，不做任何处理，也不启动任何界面。
        if (intent.action == ACTION_NOOP) return
        super.onReceive(context, intent)
    }

    private fun noopIntent(context: Context): PendingIntent {
        val intent = Intent(context, TransparentWidgetProvider::class.java)
            .setAction(ACTION_NOOP)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val ACTION_NOOP = "me.jegxiao.honortoolbox.widget.TRANSPARENT_NOOP"
    }
}