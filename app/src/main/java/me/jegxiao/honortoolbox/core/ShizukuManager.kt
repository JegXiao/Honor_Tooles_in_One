package me.jegxiao.honortoolbox.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuAccess
import rikka.shizuku.ShizukuRemoteProcess
import java.util.concurrent.TimeUnit

/**
 * 对 Shizuku 的统一封装。
 *
 * Shizuku 让本应用以 shell（uid 2000）身份执行命令，因此无需 root 即可
 * 修改系统设置、切换导航栏 overlay、调整 MagicOS 性能模式。
 */
object ShizukuManager {

    const val PACKAGE_NAME = "moe.shizuku.privileged.api"

    const val REQUEST_CODE = 7_001

    enum class State { NOT_INSTALLED, NOT_RUNNING, NOT_GRANTED, GRANTED }

    fun isInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(PACKAGE_NAME, 0)
    }.isSuccess

    fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean {
        if (!isRunning()) return false
        return runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    fun state(context: Context): State = when {
        !isInstalled(context) -> State.NOT_INSTALLED
        !isRunning() -> State.NOT_RUNNING
        hasPermission() -> State.GRANTED
        else -> State.NOT_GRANTED
    }

    fun requestPermission() {
        if (!isRunning()) return
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
    }

    /** 打开 Shizuku App；未安装时返回 false。 */
    fun openShizuku(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    /**
     * 以 shell 身份执行一条 sh 命令，并读取 stdout。
     * 必须在工作线程调用。
     */
    fun runShell(command: String, timeoutMs: Long = 8_000L): ShellResult {
        if (!hasPermission()) return ShellResult.NoPermission

        var process: Process? = null
        return try {
            val p = ShizukuAccess.newProcess(arrayOf("sh", "-c", command), null, null)
            process = p

            // ShizukuRemoteProcess.exitValue() 在进程存活时抛 RuntimeException，
            // 所以用带超时的 waitForTimeout 而不是轮询 exitValue()。
            val exited = if (p is ShizukuRemoteProcess) {
                p.waitForTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            } else {
                waitForLegacy(p, timeoutMs)
            }

            if (!exited) {
                runCatching { p.destroy() }
                ShellResult.Error("Shizuku 命令执行超时")
            } else {
                val exitCode = runCatching { p.exitValue() }.getOrDefault(-1)
                val output = runCatching {
                    p.inputStream.bufferedReader().use { it.readText() }
                }.getOrDefault("")
                ShellResult.Finished(exitCode, output)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            ShellResult.Error("Shizuku 命令执行被中断")
        } catch (e: Throwable) {
            ShellResult.Error(e.message ?: "Shizuku 调用失败")
        } finally {
            runCatching { process?.destroy() }
        }
    }

    private fun waitForLegacy(process: Process, timeoutMs: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                process.exitValue()
                return true
            } catch (_: IllegalThreadStateException) {
                Thread.sleep(50)
            }
        }
        return false
    }
}

sealed class ShellResult {
    data class Finished(val exitCode: Int, val output: String) : ShellResult()
    object NoPermission : ShellResult()
    data class Error(val message: String) : ShellResult()

    val succeeded: Boolean
        get() = this is Finished && exitCode == 0
}