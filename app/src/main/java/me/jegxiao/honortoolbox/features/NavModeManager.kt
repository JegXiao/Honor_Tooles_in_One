package me.jegxiao.honortoolbox.features

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import me.jegxiao.honortoolbox.R
import me.jegxiao.honortoolbox.core.ShizukuManager
import me.jegxiao.honortoolbox.core.ShellResult
import java.util.Locale

enum class NavMode { THREE_BUTTON, TWO_BUTTON, GESTURE }

/**
 * 读写系统导航方式。
 *
 * AOSP 使用 Settings.Secure "navigation_mode"：0 = 三键，1 = 二键，2 = 手势。
 * 荣耀 / 华为使用自己的值（101 = 三键，105 = 手势），并且其 SystemUI 不会仅凭设置项
 * 立刻切换，需要同时启用 / 停用导航栏 overlay，这必须借助 Shizuku 的 shell 身份。
 */
object NavModeManager {

    private const val KEY_NAVIGATION_MODE = "navigation_mode"
    private const val KEY_HONOR_GESTURE = "secure_gesture_navigation"

    private const val AOSP_THREE_BUTTON = 0
    private const val AOSP_TWO_BUTTON = 1
    private const val AOSP_GESTURE = 2

    private const val HONOR_THREE_BUTTON = 101
    private const val HONOR_GESTURE = 105

    private const val OVERLAY_GESTURAL = "com.android.internal.systemui.navbar.gestural"
    private const val OVERLAY_THREE_BUTTON = "com.android.internal.systemui.navbar.threebutton"

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun isHonorFamily(): Boolean {
        val manufacturer = Build.MANUFACTURER.uppercase(Locale.ROOT)
        val brand = Build.BRAND.uppercase(Locale.ROOT)
        return manufacturer.contains("HONOR") || brand.contains("HONOR") ||
            manufacturer.contains("HUAWEI") || brand.contains("HUAWEI")
    }

    fun settingUri(): Uri = Settings.Secure.getUriFor(KEY_NAVIGATION_MODE)

    fun canWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** 当前设备上是否具备实际切换能力。 */
    fun canToggle(context: Context): Boolean = when {
        !isSupported() -> false
        // 荣耀 / 华为必须借助 Shizuku 切换 overlay，仅写设置项不会生效。
        isHonorFamily() -> ShizukuManager.hasPermission()
        else -> canWriteSecureSettings(context) || ShizukuManager.hasPermission()
    }

    fun currentMode(context: Context): NavMode {
        if (!isSupported()) return NavMode.THREE_BUTTON

        if (isHonorFamily()) {
            // secure_gesture_navigation 是荣耀设置界面同步维护的开关值。
            val gestureFlag = Settings.Secure.getInt(
                context.contentResolver, KEY_HONOR_GESTURE, -1
            )
            if (gestureFlag == 1) return NavMode.GESTURE
            if (gestureFlag == 0) return NavMode.THREE_BUTTON

            val raw = Settings.Secure.getInt(
                context.contentResolver, KEY_NAVIGATION_MODE, HONOR_THREE_BUTTON
            )
            return if (raw == HONOR_GESTURE) NavMode.GESTURE else NavMode.THREE_BUTTON
        }

        return when (Settings.Secure.getInt(
            context.contentResolver, KEY_NAVIGATION_MODE, AOSP_THREE_BUTTON
        )) {
            AOSP_GESTURE -> NavMode.GESTURE
            AOSP_TWO_BUTTON -> NavMode.TWO_BUTTON
            else -> NavMode.THREE_BUTTON
        }
    }

    fun isGesture(context: Context): Boolean = currentMode(context) == NavMode.GESTURE

    fun targetMode(context: Context): NavMode =
        if (isGesture(context)) NavMode.THREE_BUTTON else NavMode.GESTURE

    /** 必须在工作线程调用（Shizuku 命令可能阻塞数秒）。 */
    fun switchMode(context: Context): SwitchResult {
        if (!isSupported()) return SwitchResult.Unsupported

        val target = targetMode(context)
        if (isHonorFamily()) {
            if (!ShizukuManager.hasPermission()) return SwitchResult.NeedsShizuku
            return switchHonor(context, target)
        }

        if (canWriteSecureSettings(context)) {
            val direct = writeAosp(context, target)
            if (direct is SwitchResult.Success || !ShizukuManager.hasPermission()) {
                return direct
            }
        }

        if (ShizukuManager.hasPermission()) {
            return switchAospShell(context, target)
        }
        return SwitchResult.NoPermission
    }

    private fun writeAosp(context: Context, target: NavMode): SwitchResult {
        val value = when (target) {
            NavMode.GESTURE -> AOSP_GESTURE
            NavMode.TWO_BUTTON -> AOSP_TWO_BUTTON
            NavMode.THREE_BUTTON -> AOSP_THREE_BUTTON
        }
        return try {
            val written = Settings.Secure.putInt(context.contentResolver, KEY_NAVIGATION_MODE, value)
            when {
                !written -> SwitchResult.Failed(context.getString(R.string.error_write_rejected))
                currentMode(context) != target ->
                    SwitchResult.Failed(context.getString(R.string.error_not_applied))
                else -> SwitchResult.Success(target)
            }
        } catch (_: SecurityException) {
            SwitchResult.NoPermission
        } catch (e: Exception) {
            SwitchResult.Failed(e.message ?: context.getString(R.string.error_unknown))
        }
    }

    /**
     * 荣耀 / 华为：同时写入两个设置项，再用 shell 身份切换导航栏 overlay。
     */
    private fun switchHonor(context: Context, target: NavMode): SwitchResult {
        val gesture = target == NavMode.GESTURE
        val command = buildString {
            append("settings put secure ").append(KEY_NAVIGATION_MODE).append(' ')
                .append(if (gesture) HONOR_GESTURE else HONOR_THREE_BUTTON).append("; ")
            append("settings put secure ").append(KEY_HONOR_GESTURE).append(' ')
                .append(if (gesture) 1 else 0).append("; ")
            if (gesture) {
                append("cmd overlay enable-exclusive --category ")
                    .append(OVERLAY_GESTURAL).append("; ")
            } else {
                append("cmd overlay disable ").append(OVERLAY_GESTURAL).append("; ")
                append("cmd overlay disable ").append(OVERLAY_THREE_BUTTON).append("; ")
            }
            append("sleep 0.2; cmd overlay list")
        }
        return verifyShellSwitch(context, target, command, checkOverlay = true)
    }

    /** 非荣耀机型走 Shizuku 时的 AOSP 写法。 */
    private fun switchAospShell(context: Context, target: NavMode): SwitchResult {
        val value = when (target) {
            NavMode.GESTURE -> AOSP_GESTURE
            NavMode.TWO_BUTTON -> AOSP_TWO_BUTTON
            NavMode.THREE_BUTTON -> AOSP_THREE_BUTTON
        }
        val command = buildString {
            append("settings put secure ").append(KEY_NAVIGATION_MODE).append(' ')
                .append(value).append("; ")
            if (target == NavMode.GESTURE) {
                append("cmd overlay enable-exclusive --category ")
                    .append(OVERLAY_GESTURAL).append("; ")
            } else {
                append("cmd overlay disable ").append(OVERLAY_GESTURAL).append("; ")
                append("cmd overlay disable ").append(OVERLAY_THREE_BUTTON).append("; ")
            }
        }
        return verifyShellSwitch(context, target, command, checkOverlay = false)
    }

    private fun verifyShellSwitch(
        context: Context,
        target: NavMode,
        command: String,
        checkOverlay: Boolean
    ): SwitchResult {
        return when (val result = ShizukuManager.runShell(command)) {
            is ShellResult.Finished -> {
                val overlayGesture = if (checkOverlay) {
                    result.output.lineSequence()
                        .firstOrNull { it.contains("navbar.gestural") }
                        ?.trimStart()
                        ?.startsWith("[x]", ignoreCase = true) == true
                } else {
                    target == NavMode.GESTURE
                }
                val expectedOverlay = target == NavMode.GESTURE
                if (result.exitCode == 0 &&
                    overlayGesture == expectedOverlay &&
                    currentMode(context) == target
                ) {
                    SwitchResult.Success(target)
                } else {
                    SwitchResult.Failed(context.getString(R.string.error_not_applied))
                }
            }

            ShellResult.NoPermission -> SwitchResult.NeedsShizuku
            is ShellResult.Error -> SwitchResult.Failed(result.message)
        }
    }

    fun modeNameRes(mode: NavMode): Int = when (mode) {
        NavMode.GESTURE -> R.string.mode_gesture
        NavMode.TWO_BUTTON -> R.string.mode_two
        NavMode.THREE_BUTTON -> R.string.mode_three
    }

    fun modeDescRes(mode: NavMode): Int = when (mode) {
        NavMode.GESTURE -> R.string.mode_gesture_desc
        NavMode.TWO_BUTTON -> R.string.mode_two_desc
        NavMode.THREE_BUTTON -> R.string.mode_three_desc
    }

    fun modeShortNameRes(mode: NavMode): Int = when (mode) {
        NavMode.GESTURE -> R.string.mode_gesture_short
        NavMode.TWO_BUTTON -> R.string.mode_two_short
        NavMode.THREE_BUTTON -> R.string.mode_three_short
    }
}

sealed class SwitchResult {
    data class Success(val mode: NavMode) : SwitchResult()
    object NoPermission : SwitchResult()
    object NeedsShizuku : SwitchResult()
    object Unsupported : SwitchResult()
    data class Failed(val reason: String) : SwitchResult()
}