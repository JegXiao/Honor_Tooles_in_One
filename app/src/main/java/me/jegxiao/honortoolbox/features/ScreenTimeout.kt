package me.jegxiao.honortoolbox.features

import android.content.Context
import android.net.Uri
import android.provider.Settings
import me.jegxiao.honortoolbox.R

data class ScreenTimeoutOption(val millis: Int, val label: String)

/** 读写系统熄屏时间。 */
object ScreenTimeout {

    /** 快捷开关未激活时使用的时长。 */
    const val SHORT_TIMEOUT_MILLIS = 2 * 60 * 1000

    /**
     * 长时长档位。
     *
     * Android 17 / MagicOS 11 的系统设置已移除「永不熄屏」，打开设置页时会把
     * 2147483647 之类的非列表值重置成 30 秒。30 分钟是系统列表里的最大值，
     * 也是升级后仍能稳定保留的最长时长。
     */
    const val LONG_TIMEOUT_MILLIS = 30 * 60 * 1000

    fun options(context: Context): List<ScreenTimeoutOption> {
        val values = context.resources.getIntArray(R.array.screen_timeout_values)
        val labels = context.resources.getStringArray(R.array.screen_timeout_labels)
        return List(minOf(values.size, labels.size)) { index ->
            ScreenTimeoutOption(values[index], labels[index])
        }
    }

    fun settingUri(): Uri = Settings.System.getUriFor(Settings.System.SCREEN_OFF_TIMEOUT)

    fun read(context: Context): Int? = try {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT
        )
    } catch (_: Settings.SettingNotFoundException) {
        null
    }

    fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

    /** 写入后回读校验，避免部分系统静默拒绝或把值限制成其他档位。 */
    fun write(context: Context, millis: Int): Boolean {
        if (!canWrite(context)) return false
        return try {
            val accepted = Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                millis
            )
            accepted && read(context) == millis
        } catch (_: SecurityException) {
            false
        }
    }

    fun isLong(context: Context): Boolean = read(context) == LONG_TIMEOUT_MILLIS

    fun format(context: Context, millis: Int): String {
        options(context).firstOrNull { it.millis == millis }?.let { return it.label }
        if (millis > LONG_TIMEOUT_MILLIS) {
            return context.getString(R.string.timeout_over_long)
        }

        val totalSeconds = millis.coerceAtLeast(0) / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return when {
            minutes == 0 -> context.getString(R.string.timeout_seconds, seconds)
            seconds == 0 -> context.getString(R.string.timeout_minutes, minutes)
            else -> context.getString(R.string.timeout_minutes_seconds, minutes, seconds)
        }
    }

    /** 小组件和控制中心使用的极简文案。 */
    fun shortLabel(context: Context, millis: Int?): String = when {
        millis == null -> context.getString(R.string.timeout_unknown_short)
        millis > LONG_TIMEOUT_MILLIS -> context.getString(R.string.timeout_over_long_short)
        millis == LONG_TIMEOUT_MILLIS -> context.getString(R.string.timeout_long_short)
        else -> {
            val totalSeconds = millis.coerceAtLeast(0) / 1000
            when {
                totalSeconds < 60 -> context.getString(R.string.timeout_seconds_short, totalSeconds)
                totalSeconds % 60 == 0 ->
                    context.getString(R.string.timeout_minutes_short, totalSeconds / 60)
                else -> context.getString(
                    R.string.timeout_minutes_short,
                    totalSeconds / 60
                )
            }
        }
    }
}