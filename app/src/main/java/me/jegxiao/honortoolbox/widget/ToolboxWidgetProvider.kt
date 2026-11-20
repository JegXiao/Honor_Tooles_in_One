package me.jegxiao.honortoolbox.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import me.jegxiao.honortoolbox.MainActivity
import me.jegxiao.honortoolbox.R
import me.jegxiao.honortoolbox.ScreenTimeoutActivity
import me.jegxiao.honortoolbox.SetupActivity
import me.jegxiao.honortoolbox.core.RootShell
import me.jegxiao.honortoolbox.features.NavMode
import me.jegxiao.honortoolbox.features.NavModeManager
import me.jegxiao.honortoolbox.features.PerfMode
import me.jegxiao.honortoolbox.features.ScreenTimeout
import me.jegxiao.honortoolbox.features.SwitchResult
import me.jegxiao.honortoolbox.features.WirelessDebug

/**
 * 2×2 桌面小组件：四个功能各占一格，点按即可切换。
 * 未授权时点按会自动打开「授权与帮助」。
 */
class ToolboxWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null || action !in ALL_ACTIONS) {
            super.onReceive(context, intent)
            return
        }

        val pendingResult = goAsync()
        Thread {
            try {
                handleAction(context, action)
            } finally {
                updateAll(context)
                pendingResult.finish()
            }
        }.start()
    }

    private fun handleAction(context: Context, action: String) {
        when (action) {
            ACTION_NAV -> toggleNav(context)
            ACTION_TIMEOUT -> toggleTimeout(context)
            ACTION_WIRELESS -> toggleWireless(context)
            ACTION_PERF -> togglePerf(context)
        }
    }

    private fun toggleNav(context: Context) {
        when {
            !NavModeManager.isSupported() -> toast(context, R.string.toast_nav_unsupported)
            !NavModeManager.canToggle(context) -> openSetup(context, SECTION_NAV)
            else -> when (val result = NavModeManager.switchMode(context)) {
                is SwitchResult.Success -> toast(
                    context,
                    if (result.mode == NavMode.GESTURE) R.string.toast_nav_switched_gesture
                    else R.string.toast_nav_switched_three
                )

                SwitchResult.NoPermission,
                SwitchResult.NeedsShizuku -> openSetup(context, SECTION_NAV)

                SwitchResult.Unsupported -> toast(context, R.string.toast_nav_unsupported)
                is SwitchResult.Failed -> toast(context, result.reason)
            }
        }
    }

    private fun toggleTimeout(context: Context) {
        if (!ScreenTimeout.canWrite(context)) {
            openActivity(context, Intent(context, ScreenTimeoutActivity::class.java))
            return
        }
        val current = ScreenTimeout.read(context)
        val target = if (current == ScreenTimeout.NEVER_TIMEOUT_MILLIS) {
            ScreenTimeout.SHORT_TIMEOUT_MILLIS
        } else {
            ScreenTimeout.NEVER_TIMEOUT_MILLIS
        }
        val ok = ScreenTimeout.write(context, target)
        toast(
            context,
            if (!ok) {
                R.string.toast_timeout_failed
            } else if (target == ScreenTimeout.NEVER_TIMEOUT_MILLIS) {
                R.string.toast_timeout_never
            } else {
                R.string.toast_timeout_short
            }
        )
    }

    private fun toggleWireless(context: Context) {
        if (!WirelessDebug.isSupported()) {
            toast(context, R.string.toast_wireless_unsupported)
            return
        }
        if (!WirelessDebug.canToggle(context)) {
            openSetup(context, SetupActivity.SECTION_SHIZUKU)
            return
        }
        val target = !WirelessDebug.isEnabled(context)
        when (val result = WirelessDebug.setEnabledBlocking(context, target)) {
            is WirelessDebug.Result.Success -> toast(
                context,
                if (target) R.string.toast_wireless_enabled
                else R.string.toast_wireless_disabled
            )

            WirelessDebug.Result.Unsupported ->
                toast(context, R.string.toast_wireless_unsupported)

            WirelessDebug.Result.PermissionMissing,
            WirelessDebug.Result.ShizukuFailed,
            WirelessDebug.Result.Failed -> {
                toast(context, R.string.toast_wireless_failed)
                openSetup(context, SetupActivity.SECTION_SHIZUKU)
            }
        }
    }

    private fun togglePerf(context: Context) {
        if (!PerfMode.isSupported(context)) {
            toast(context, R.string.toast_perf_unsupported)
            return
        }
        if (!PerfMode.canWriteAny(context)) {
            openSetup(context, SetupActivity.SECTION_SHIZUKU)
            return
        }
        val target = !PerfMode.isPerformance(context)
        val ok = PerfMode.writeModeBlocking(context, target)
        if (!ok) {
            toast(context, R.string.toast_perf_failed)
        }
    }

    private fun openSetup(context: Context, section: String) {
        openActivity(
            context,
            Intent(context, SetupActivity::class.java)
                .putExtra(SetupActivity.EXTRA_SECTION, section)
        )
    }

    private fun openActivity(context: Context, intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        mainHandler.post {
            runCatching { context.startActivity(intent) }
        }
    }

    private fun toast(context: Context, resId: Int) {
        mainHandler.post {
            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toast(context: Context, text: CharSequence) {
        mainHandler.post {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ACTION_NAV = "me.jegxiao.honortoolbox.widget.NAV"
        private const val ACTION_TIMEOUT = "me.jegxiao.honortoolbox.widget.TIMEOUT"
        private const val ACTION_WIRELESS = "me.jegxiao.honortoolbox.widget.WIRELESS"
        private const val ACTION_PERF = "me.jegxiao.honortoolbox.widget.PERF"

        private const val SECTION_NAV = "nav"

        private val ALL_ACTIONS = setOf(ACTION_NAV, ACTION_TIMEOUT, ACTION_WIRELESS, ACTION_PERF)

        private val mainHandler = Handler(Looper.getMainLooper())

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ToolboxWidgetProvider::class.java)
            )
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_toolbox)
            val activeColor = context.getColor(R.color.widget_active)
            val inactiveColor = context.getColor(R.color.widget_inactive)

            // 导航
            val navSupported = NavModeManager.isSupported()
            val navMode = NavModeManager.currentMode(context)
            val navGesture = navMode == NavMode.GESTURE
            views.setImageViewResource(
                R.id.widget_nav_icon,
                when {
                    !navSupported -> R.drawable.ic_nav_switch
                    navGesture -> R.drawable.ic_nav_gesture
                    else -> R.drawable.ic_nav_three
                }
            )
            views.setTextViewText(
                R.id.widget_nav_label,
                context.getString(
                    when {
                        !navSupported -> R.string.widget_unsupported_short
                        navGesture -> R.string.widget_nav_gesture_short
                        else -> R.string.widget_nav_three_short
                    }
                )
            )
            tint(views, R.id.widget_nav_icon, R.id.widget_nav_label,
                if (navSupported && navGesture) activeColor else inactiveColor)
            views.setOnClickPendingIntent(R.id.widget_nav_cell, action(context, ACTION_NAV, 21))

            // 熄屏
            val timeout = ScreenTimeout.read(context)
            val never = timeout == ScreenTimeout.NEVER_TIMEOUT_MILLIS
            views.setImageViewResource(R.id.widget_timeout_icon, R.drawable.ic_screen_timeout)
            views.setTextViewText(
                R.id.widget_timeout_label,
                ScreenTimeout.shortLabel(context, timeout)
            )
            tint(views, R.id.widget_timeout_icon, R.id.widget_timeout_label,
                if (never) activeColor else inactiveColor)
            views.setOnClickPendingIntent(
                R.id.widget_timeout_cell,
                action(context, ACTION_TIMEOUT, 22)
            )

            // 无线调试
            val wirelessSupported = WirelessDebug.isSupported()
            val wirelessOn = wirelessSupported && WirelessDebug.isEnabled(context)
            views.setImageViewResource(
                R.id.widget_wireless_icon,
                R.drawable.ic_wireless_debug
            )
            views.setTextViewText(
                R.id.widget_wireless_label,
                context.getString(
                    when {
                        !wirelessSupported -> R.string.widget_unsupported_short
                        wirelessOn -> R.string.widget_on_short
                        else -> R.string.widget_off_short
                    }
                )
            )
            tint(views, R.id.widget_wireless_icon, R.id.widget_wireless_label,
                if (wirelessOn) activeColor else inactiveColor)
            views.setOnClickPendingIntent(
                R.id.widget_wireless_cell,
                action(context, ACTION_WIRELESS, 23)
            )

            // 性能模式
            val perfSupported = PerfMode.isSupported(context)
            val perfOn = perfSupported && PerfMode.isPerformance(context)
            views.setImageViewResource(R.id.widget_perf_icon, R.drawable.ic_performance)
            views.setTextViewText(
                R.id.widget_perf_label,
                context.getString(
                    when {
                        !perfSupported -> R.string.widget_unsupported_short
                        perfOn -> R.string.widget_perf_on_short
                        else -> R.string.widget_perf_off_short
                    }
                )
            )
            tint(views, R.id.widget_perf_icon, R.id.widget_perf_label,
                if (perfOn) activeColor else inactiveColor)
            views.setOnClickPendingIntent(R.id.widget_perf_cell, action(context, ACTION_PERF, 24))

            manager.updateAppWidget(widgetId, views)
        }

        private fun tint(views: RemoteViews, iconId: Int, labelId: Int, color: Int) {
            views.setInt(iconId, "setColorFilter", color)
            views.setTextColor(labelId, color)
        }

        private fun action(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, ToolboxWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}