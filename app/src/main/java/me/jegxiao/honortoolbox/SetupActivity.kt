package me.jegxiao.honortoolbox

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import me.jegxiao.honortoolbox.core.RootShell
import me.jegxiao.honortoolbox.core.ShizukuManager
import me.jegxiao.honortoolbox.core.applyEdgeToEdge
import me.jegxiao.honortoolbox.core.requestTileRefresh
import me.jegxiao.honortoolbox.features.PerfMode
import me.jegxiao.honortoolbox.features.ScreenTimeout
import me.jegxiao.honortoolbox.features.WirelessDebug
import me.jegxiao.honortoolbox.widget.ToolboxWidgetProvider
import rikka.shizuku.Shizuku

class SetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SECTION = "extra_section"
        const val SECTION_SHIZUKU = "shizuku"
        const val SECTION_ADB = "adb"
        const val SECTION_WRITE_SETTINGS = "write_settings"

        private const val SHIZUKU_WEBSITE = "https://shizuku.rikka.app/"
    }

    private lateinit var root: NestedScrollView
    private lateinit var cardShizuku: MaterialCardView
    private lateinit var cardAdb: MaterialCardView
    private lateinit var cardWriteSettings: MaterialCardView
    private lateinit var shizukuStatus: TextView
    private lateinit var openShizukuButton: MaterialButton
    private lateinit var grantShizukuButton: MaterialButton
    private lateinit var adbStatus: TextView
    private lateinit var adbCommand: TextView
    private lateinit var writeSettingsStatus: TextView
    private lateinit var rootButton: MaterialButton

    private val binderReceivedListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            runOnUiThread { refresh() }
        }
    }

    private val binderDeadListener = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() {
            runOnUiThread { refresh() }
        }
    }

    private val permissionResultListener =
        object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode != ShizukuManager.REQUEST_CODE) return
                runOnUiThread {
                    refresh()
                    Toast.makeText(
                        this@SetupActivity,
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            R.string.toast_shizuku_granted
                        } else {
                            R.string.toast_still_missing
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                    ToolboxWidgetProvider.updateAll(this@SetupActivity)
                    requestTileRefresh(this@SetupActivity)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        applyEdgeToEdge(findViewById(R.id.root))

        root = findViewById(R.id.root)
        cardShizuku = findViewById(R.id.card_shizuku)
        cardAdb = findViewById(R.id.card_adb)
        cardWriteSettings = findViewById(R.id.card_write_settings)
        shizukuStatus = findViewById(R.id.shizuku_status)
        openShizukuButton = findViewById(R.id.open_shizuku_button)
        grantShizukuButton = findViewById(R.id.grant_shizuku_button)
        adbStatus = findViewById(R.id.adb_status)
        adbCommand = findViewById(R.id.adb_command)
        writeSettingsStatus = findViewById(R.id.write_settings_status)
        rootButton = findViewById(R.id.root_button)
        val versionText = findViewById<TextView>(R.id.version_text)

        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }
        adbCommand.text = getString(R.string.adb_command, packageName)
        versionText.text = getString(R.string.version_label, appVersionName())

        openShizukuButton.setOnClickListener { onOpenShizukuClick() }
        grantShizukuButton.setOnClickListener { onGrantShizukuClick() }
        findViewById<MaterialButton>(R.id.copy_button).setOnClickListener { copyAdbCommand() }
        findViewById<MaterialButton>(R.id.recheck_button).setOnClickListener {
            refresh()
            val granted = WirelessDebug.hasWriteSecureSettings(this) ||
                ScreenTimeout.canWrite(this)
            Toast.makeText(
                this,
                if (granted) R.string.toast_granted else R.string.toast_still_missing,
                Toast.LENGTH_SHORT
            ).show()
        }
        rootButton.setOnClickListener { requestRootGrant() }
        findViewById<MaterialButton>(R.id.write_settings_button).setOnClickListener {
            openWriteSettingsPermission()
        }
        findViewById<MaterialButton>(R.id.battery_settings_button).setOnClickListener {
            PerfMode.openBatterySettings(this)
        }

        runCatching { Shizuku.addBinderReceivedListenerSticky(binderReceivedListener) }
        runCatching { Shizuku.addBinderDeadListener(binderDeadListener) }
        runCatching { Shizuku.addRequestPermissionResultListener(permissionResultListener) }

        refresh()
        scrollToSection(intent.getStringExtra(EXTRA_SECTION))
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onDestroy() {
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionResultListener) }
        runCatching { Shizuku.removeBinderDeadListener(binderDeadListener) }
        runCatching { Shizuku.removeBinderReceivedListener(binderReceivedListener) }
        super.onDestroy()
    }

    private fun refresh() {
        if (isFinishing || isDestroyed) return

        val state = ShizukuManager.state(this)
        shizukuStatus.setText(
            when (state) {
                ShizukuManager.State.NOT_INSTALLED -> R.string.shizuku_status_not_installed
                ShizukuManager.State.NOT_RUNNING -> R.string.shizuku_status_not_running
                ShizukuManager.State.NOT_GRANTED -> R.string.shizuku_status_not_granted
                ShizukuManager.State.GRANTED -> R.string.shizuku_status_granted
            }
        )
        when (state) {
            ShizukuManager.State.NOT_INSTALLED -> {
                openShizukuButton.setText(R.string.shizuku_get)
                openShizukuButton.isEnabled = true
                grantShizukuButton.setText(R.string.shizuku_grant)
                grantShizukuButton.isEnabled = false
            }

            ShizukuManager.State.NOT_RUNNING -> {
                openShizukuButton.setText(R.string.shizuku_open)
                openShizukuButton.isEnabled = true
                grantShizukuButton.setText(R.string.shizuku_grant)
                grantShizukuButton.isEnabled = false
            }

            ShizukuManager.State.NOT_GRANTED -> {
                openShizukuButton.setText(R.string.shizuku_open)
                openShizukuButton.isEnabled = true
                grantShizukuButton.setText(R.string.shizuku_grant)
                grantShizukuButton.isEnabled = true
            }

            ShizukuManager.State.GRANTED -> {
                openShizukuButton.setText(R.string.shizuku_open)
                openShizukuButton.isEnabled = true
                grantShizukuButton.setText(R.string.shizuku_granted)
                grantShizukuButton.isEnabled = false
            }
        }

        val secureGranted = WirelessDebug.hasWriteSecureSettings(this)
        adbStatus.setText(
            if (secureGranted) R.string.adb_status_granted
            else R.string.adb_status_missing
        )

        val writeGranted = ScreenTimeout.canWrite(this)
        writeSettingsStatus.setText(
            if (writeGranted) R.string.write_settings_status_granted
            else R.string.write_settings_status_missing
        )
    }

    private fun onOpenShizukuClick() {
        if (ShizukuManager.state(this) == ShizukuManager.State.NOT_INSTALLED) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_WEBSITE)))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.toast_shizuku_not_installed, Toast.LENGTH_LONG).show()
            }
            return
        }
        if (!ShizukuManager.openShizuku(this)) {
            Toast.makeText(this, R.string.toast_shizuku_not_installed, Toast.LENGTH_LONG).show()
        }
    }

    private fun onGrantShizukuClick() {
        when (ShizukuManager.state(this)) {
            ShizukuManager.State.GRANTED ->
                Toast.makeText(this, R.string.toast_shizuku_granted, Toast.LENGTH_SHORT).show()

            ShizukuManager.State.NOT_RUNNING ->
                Toast.makeText(this, R.string.toast_shizuku_not_running, Toast.LENGTH_SHORT).show()

            ShizukuManager.State.NOT_INSTALLED ->
                Toast.makeText(this, R.string.toast_shizuku_not_installed, Toast.LENGTH_LONG).show()

            ShizukuManager.State.NOT_GRANTED -> ShizukuManager.requestPermission()
        }
    }

    private fun copyAdbCommand() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                getString(R.string.adb_title),
                adbCommand.text.toString()
            )
        )
        Toast.makeText(this, R.string.toast_copied, Toast.LENGTH_SHORT).show()
    }

    private fun requestRootGrant() {
        rootButton.isEnabled = false
        Toast.makeText(this, R.string.toast_root_trying, Toast.LENGTH_SHORT).show()
        Thread {
            val granted = RootShell.grantPermissions(applicationContext)
            runOnUiThread {
                rootButton.isEnabled = true
                refresh()
                ToolboxWidgetProvider.updateAll(this)
                requestTileRefresh(this)
                Toast.makeText(
                    this,
                    if (granted) R.string.toast_root_granted else R.string.toast_root_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.start()
    }

    private fun openWriteSettingsPermission() {
        val packageUri = "package:$packageName".toUri()
        val candidates = listOf(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, packageUri),
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // 尝试下一个入口。
            } catch (_: SecurityException) {
                // 部分定制系统会拦截，继续回退。
            }
        }
        Toast.makeText(this, R.string.write_settings_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun scrollToSection(section: String?) {
        val target = when (section) {
            SECTION_SHIZUKU -> cardShizuku
            SECTION_ADB -> cardAdb
            SECTION_WRITE_SETTINGS -> cardWriteSettings
            else -> null
        } ?: return
        root.post {
            root.smoothScrollTo(0, target.top)
        }
    }

    private fun appVersionName(): String = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }
}