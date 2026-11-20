package me.jegxiao.honortoolbox.features

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import me.jegxiao.honortoolbox.core.RootShell
import me.jegxiao.honortoolbox.core.ShizukuManager
import me.jegxiao.honortoolbox.core.ShellResult
import java.util.concurrent.Executors

/**
 * 负责读写系统「无线调试」开关。
 *
 * Android 11 起，无线调试的实际开关是 Settings.Global 中的隐藏项
 * `adb_wifi_enabled`。AdbService 会监听该值并启动 / 停止 adbd 的无线调试通道。
 * 写入需要特权：优先 Shizuku，其次 adb 授予的 WRITE_SECURE_SETTINGS，最后 root。
 */
object WirelessDebug {

    const val SETTING_ADB_WIFI_ENABLED = "adb_wifi_enabled"

    val MIN_API: Int = Build.VERSION_CODES.R

    enum class Backend { SHIZUKU, DIRECT, ROOT }

    sealed class Result {
        data class Success(val backend: Backend) : Result()
        object Unsupported : Result()
        object PermissionMissing : Result()
        object ShizukuFailed : Result()
        object Failed : Result()
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun settingUri(): Uri = Settings.Global.getUriFor(SETTING_ADB_WIFI_ENABLED)

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= MIN_API

    fun isEnabled(context: Context): Boolean {
        if (!isSupported()) return false
        return runCatching {
            Settings.Global.getInt(
                context.contentResolver,
                SETTING_ADB_WIFI_ENABLED,
                0
            ) != 0
        }.getOrDefault(false)
    }

    fun hasWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** 是否具备至少一种写入方式（root 无法静默探测，不参与判断）。 */
    fun canToggle(context: Context): Boolean =
        isSupported() && (ShizukuManager.hasPermission() || hasWriteSecureSettings(context))

    /** 把开关切换到 [enabled]。回调保证在主线程。 */
    fun setEnabled(context: Context, enabled: Boolean, callback: (Result) -> Unit) {
        if (!isSupported()) {
            mainHandler.post { callback(Result.Unsupported) }
            return
        }
        val appContext = context.applicationContext
        executor.execute {
            val result = setEnabledBlocking(appContext, enabled)
            mainHandler.post { callback(result) }
        }
    }

    /**
     * 同步切换，供小组件或工作线程使用。
     * Shizuku 失败时仍会尝试直接写入与 root，避免误报「缺少权限」。
     */
    fun setEnabledBlocking(context: Context, enabled: Boolean): Result {
        if (!isSupported()) return Result.Unsupported

        val target = if (enabled) 1 else 0
        var shizukuAttempted = false
        var shizukuFailed = false

        if (ShizukuManager.hasPermission()) {
            shizukuAttempted = true
            val shell = ShizukuManager.runShell(
                "settings put global $SETTING_ADB_WIFI_ENABLED $target"
            )
            if (shell.succeeded && isEnabled(context) == enabled) {
                return Result.Success(Backend.SHIZUKU)
            }
            shizukuFailed = true
        }

        val canWrite = hasWriteSecureSettings(context)
        if (canWrite) {
            val written = runCatching {
                Settings.Global.putInt(
                    context.contentResolver,
                    SETTING_ADB_WIFI_ENABLED,
                    target
                )
            }.getOrDefault(false)
            if (written && isEnabled(context) == enabled) {
                return Result.Success(Backend.DIRECT)
            }
        }

        if (RootShell.putGlobal(SETTING_ADB_WIFI_ENABLED, target) &&
            isEnabled(context) == enabled
        ) {
            return Result.Success(Backend.ROOT)
        }

        return when {
            shizukuAttempted && shizukuFailed -> Result.ShizukuFailed
            !canWrite && !ShizukuManager.hasPermission() -> Result.PermissionMissing
            else -> Result.Failed
        }
    }
}