package me.jegxiao.honortoolbox.core

import android.content.Context

/**
 * Root 备用通道。只有在用户主动点击「尝试 Root 授权」或 Shizuku 不可用时才会调用，
 * 且必须在工作线程执行。
 */
object RootShell {

    private const val TIMEOUT_MS = 8_000L

    /** 一次性授予安全设置与「修改系统设置」权限。 */
    fun grantPermissions(context: Context): Boolean {
        val pkg = context.packageName
        val command = "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS; " +
            "appops set $pkg android:write_settings allow"
        return runSu(command)
    }

    fun putGlobal(key: String, value: Int): Boolean =
        runSu("settings put global $key $value")

    fun putSystem(key: String, value: Int): Boolean =
        runSu("settings put system $key $value")

    private fun runSu(command: String): Boolean {
        var process: Process? = null
        return try {
            val p = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            process = p
            // 命令没有输入，及时关闭 stdin。
            runCatching { p.outputStream.close() }

            val deadline = System.currentTimeMillis() + TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                try {
                    return p.exitValue() == 0
                } catch (_: IllegalThreadStateException) {
                    Thread.sleep(100)
                }
            }
            false
        } catch (_: Exception) {
            false
        } finally {
            runCatching { process?.destroy() }
        }
    }
}