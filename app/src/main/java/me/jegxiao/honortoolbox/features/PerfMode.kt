package me.jegxiao.honortoolbox.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import me.jegxiao.honortoolbox.core.RootShell
import me.jegxiao.honortoolbox.core.ShizukuManager
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 荣耀 MagicOS / 华为 EMUI 的电池使用模式存放在 Settings.System.SmartModeStatus：
 * 5 = 性能模式，1 = 均衡模式。
 *
 * MagicOS 会拒绝第三方应用直接写入系统设置（即使拥有 WRITE_SETTINGS），因此优先
 * 通过 Shizuku 以 shell 身份执行 `settings put system SmartModeStatus 5`。
 */
object PerfMode {

    const val KEY = "SmartModeStatus"

    const val MODE_BALANCED = 1
    const val MODE_PERFORMANCE = 5

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 当前原始值；系统没有该设置项时返回 null。 */
    fun readMode(context: Context): Int? {
        val resolver = context.contentResolver
        return try {
            if (Settings.System.getString(resolver, KEY) == null) {
                null
            } else {
                Settings.System.getInt(resolver, KEY, MODE_BALANCED)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun isPerformance(context: Context): Boolean = readMode(context) == MODE_PERFORMANCE

    fun isSupported(context: Context): Boolean = readMode(context) != null

    /** 「修改系统设置」特殊权限是否已授予。 */
    fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

    /** 是否存在可用的写入通道。 */
    fun canWriteAny(context: Context): Boolean =
        ShizukuManager.hasPermission() || canWrite(context)

    fun isHonorOrHuawei(): Boolean {
        val brand = Build.BRAND.lowercase(Locale.ROOT)
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
        return brand == "honor" || manufacturer == "honor" ||
            brand == "huawei" || manufacturer == "huawei"
    }

    fun settingsUri(): Uri = Settings.System.getUriFor(KEY)

    /** 工作线程写入并回读校验。 */
    fun writeModeBlocking(context: Context, enabled: Boolean): Boolean {
        val target = if (enabled) MODE_PERFORMANCE else MODE_BALANCED

        val shellOk = if (ShizukuManager.hasPermission()) {
            ShizukuManager.runShell(
                "settings put system $KEY $target"
            ).succeeded
        } else {
            false
        }
        if (shellOk && readMode(context) == target) return true

        if (canWrite(context)) {
            val written = runCatching {
                Settings.System.putInt(context.contentResolver, KEY, target)
            }.getOrDefault(false)
            if (written && readMode(context) == target) return true
        }

        if (RootShell.putSystem(KEY, target) && readMode(context) == target) {
            return true
        }
        return false
    }

    /** 异步写入，回调保证在主线程。 */
    fun writeMode(context: Context, enabled: Boolean, callback: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        executor.execute {
            val ok = writeModeBlocking(appContext, enabled)
            mainHandler.post { callback(ok) }
        }
    }

    /** 打开系统电池页面（厂商页面通常不对外导出）。 */
    fun openBatterySettings(context: Context) {
        val candidates = listOf(
            Intent("android.intent.action.POWER_USAGE_SUMMARY"),
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // 尝试下一个入口。
            }
        }
    }
}