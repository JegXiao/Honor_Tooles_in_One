package me.jegxiao.honortoolbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import me.jegxiao.honortoolbox.core.ShizukuManager
import me.jegxiao.honortoolbox.core.applyEdgeToEdge
import me.jegxiao.honortoolbox.core.requestTileRefresh
import me.jegxiao.honortoolbox.features.NavMode
import me.jegxiao.honortoolbox.features.NavModeManager
import me.jegxiao.honortoolbox.features.PerfMode
import me.jegxiao.honortoolbox.features.ScreenTimeout
import me.jegxiao.honortoolbox.features.SwitchResult
import me.jegxiao.honortoolbox.features.WirelessDebug
import me.jegxiao.honortoolbox.widget.ToolboxWidgetProvider
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_TIMEOUT = "extra_open_timeout"
        const val EXTRA_SETUP_SECTION = "extra_setup_section"
    }

    private lateinit var navIcon: android.widget.ImageView
    private lateinit var navState: TextView
    private lateinit var navDesc: TextView
    private lateinit var navButton: MaterialButton
    private lateinit var navProgress: LinearProgressIndicator
    private lateinit var timeoutState: TextView
    private lateinit var wirelessState: TextView
    private lateinit var wirelessDesc: TextView
    private lateinit var switchWireless: MaterialSwitch
    private lateinit var wirelessProgress: LinearProgressIndicator
    private lateinit var perfState: TextView
    private lateinit var perfDesc: TextView
    private lateinit var switchPerf: MaterialSwitch
    private lateinit var perfProgress: LinearProgressIndicator
    private lateinit var setupState: TextView
    private lateinit var versionText: TextView

    private var navBusy = false
    private var wirelessBusy = false
    private var perfBusy = false
    private var pendingWirelessTarget: Boolean? = null
    private var pendingPerfTarget: Boolean? = null
    private var updatingSwitches = false

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            refreshUi()
            updateWidget()
            requestTileRefresh(this@MainActivity)
        }
    }

    private val binderReceivedListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            runOnUiThread { refreshUi() }
        }
    }

    private val binderDeadListener = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() {
            runOnUiThread { refreshUi() }
        }
    }

    private val permissionResultListener =
        object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode != ShizukuManager.REQUEST_CODE) return
                runOnUiThread {
                    refreshUi()
                    updateWidget()
                    requestTileRefresh(this@MainActivity)
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        snackbar(R.string.toast_shizuku_granted)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyEdgeToEdge(findViewById(R.id.root))
        bindViews()
        setupUi()
        registerShizukuListeners()
        refreshUi()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        listOf(
            NavModeManager.settingUri(),
            ScreenTimeout.settingUri(),
            WirelessDebug.settingUri(),
            PerfMode.settingsUri()
        ).forEach { uri ->
            contentResolver.registerContentObserver(uri, false, settingsObserver)
        }
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(settingsObserver)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
        updateWidget()
    }

    override fun onDestroy() {
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionResultListener) }
        runCatching { Shizuku.removeBinderDeadListener(binderDeadListener) }
        runCatching { Shizuku.removeBinderReceivedListener(binderReceivedListener) }
        super.onDestroy()
    }

    private fun bindViews() {
        navIcon = findViewById(R.id.nav_icon)
        navState = findViewById(R.id.nav_state)
        navDesc = findViewById(R.id.nav_desc)
        navButton = findViewById(R.id.nav_button)
        navProgress = findViewById(R.id.nav_progress)
        timeoutState = findViewById(R.id.timeout_state)
        wirelessState = findViewById(R.id.wireless_state)
        wirelessDesc = findViewById(R.id.wireless_desc)
        switchWireless = findViewById(R.id.switch_wireless)
        wirelessProgress = findViewById(R.id.wireless_progress)
        perfState = findViewById(R.id.perf_state)
        perfDesc = findViewById(R.id.perf_desc)
        switchPerf = findViewById(R.id.switch_perf)
        perfProgress = findViewById(R.id.perf_progress)
        setupState = findViewById(R.id.setup_state)
        versionText = findViewById(R.id.version_text)
    }

    private fun setupUi() {
        navButton.setOnClickListener { onNavClick() }
        findViewById<android.view.View>(R.id.card_timeout).setOnClickListener {
            startActivity(Intent(this, ScreenTimeoutActivity::class.java))
        }
        findViewById<android.view.View>(R.id.card_setup).setOnClickListener {
            openSetup(SetupActivity.SECTION_SHIZUKU)
        }
        switchWireless.setOnCheckedChangeListener { _, checked ->
            if (updatingSwitches) return@setOnCheckedChangeListener
            if (wirelessBusy) {
                refreshUi()
                return@setOnCheckedChangeListener
            }
            if (checked == WirelessDebug.isEnabled(this)) return@setOnCheckedChangeListener
            performWirelessToggle(checked)
        }
        switchPerf.setOnCheckedChangeListener { _, checked ->
            if (updatingSwitches) return@setOnCheckedChangeListener
            if (perfBusy) {
                refreshUi()
                return@setOnCheckedChangeListener
            }
            if (checked == PerfMode.isPerformance(this)) return@setOnCheckedChangeListener
            performPerfToggle(checked)
        }
        versionText.text = getString(R.string.version_label, appVersionName())
        versionText.setOnClickListener { copyVersionInfo() }
    }

    private fun registerShizukuListeners() {
        runCatching { Shizuku.addBinderReceivedListenerSticky(binderReceivedListener) }
        runCatching { Shizuku.addBinderDeadListener(binderDeadListener) }
        runCatching { Shizuku.addRequestPermissionResultListener(permissionResultListener) }
    }

    private fun refreshUi() {
        if (isFinishing || isDestroyed) return

        // 导航方式
        val navSupported = NavModeManager.isSupported()
        val navMode = NavModeManager.currentMode(this)
        val gesture = navMode == NavMode.GESTURE
        val navToggleable = navSupported && NavModeManager.canToggle(this)

        navIcon.setImageResource(
            when {
                !navSupported -> R.drawable.ic_nav_switch
                gesture -> R.drawable.ic_nav_gesture
                else -> R.drawable.ic_nav_three
            }
        )
        navState.text = getString(
            if (navSupported) NavModeManager.modeNameRes(navMode) else R.string.nav_unsupported
        )
        navDesc.text = getString(
            if (navSupported) NavModeManager.modeDescRes(navMode) else R.string.nav_unsupported_desc
        )
        navButton.text = getString(
            when {
                !navSupported -> R.string.nav_unsupported
                !navToggleable -> R.string.nav_go_setup
                gesture -> R.string.nav_switch_to_three
                else -> R.string.nav_switch_to_gesture
            }
        )
        navButton.isEnabled = navSupported && !navBusy
        navProgress.isVisible = navBusy

        // 熄屏时间
        timeoutState.text = ScreenTimeout.read(this)
            ?.let { ScreenTimeout.format(this, it) }
            ?: getString(R.string.timeout_unknown)

        // 无线调试
        val wirelessSupported = WirelessDebug.isSupported()
        val wirelessEnabled = wirelessSupported && WirelessDebug.isEnabled(this)
        val wirelessShown = pendingWirelessTarget ?: wirelessEnabled

        updatingSwitches = true
        switchWireless.isChecked = wirelessShown
        switchWireless.isEnabled = wirelessSupported && !wirelessBusy
        updatingSwitches = false

        wirelessState.setText(
            when {
                !wirelessSupported -> R.string.wireless_state_unsupported
                wirelessBusy && pendingWirelessTarget == true -> R.string.wireless_state_enabling
                wirelessBusy -> R.string.wireless_state_disabling
                wirelessEnabled -> R.string.wireless_state_on
                else -> R.string.wireless_state_off
            }
        )
        wirelessDesc.setText(
            when {
                !wirelessSupported -> R.string.wireless_desc_unsupported
                wirelessBusy -> R.string.wireless_desc_busy
                wirelessEnabled -> R.string.wireless_desc_on
                else -> R.string.wireless_desc_off
            }
        )
        wirelessProgress.isVisible = wirelessBusy

        // 性能模式
        val perfMode = PerfMode.readMode(this)
        val perfSupported = perfMode != null
        val perfOn = perfMode == PerfMode.MODE_PERFORMANCE
        val perfShown = pendingPerfTarget ?: perfOn

        updatingSwitches = true
        switchPerf.isChecked = perfShown
        switchPerf.isEnabled = perfSupported && !perfBusy
        updatingSwitches = false

        perfState.setText(
            when {
                !perfSupported -> R.string.perf_state_unsupported
                perfBusy && pendingPerfTarget == true -> R.string.perf_state_on
                perfBusy -> R.string.perf_state_off
                perfOn -> R.string.perf_state_on
                else -> R.string.perf_state_off
            }
        )
        perfDesc.setText(
            when {
                !perfSupported -> R.string.perf_desc_unsupported
                perfBusy -> R.string.perf_desc_busy
                perfOn -> R.string.perf_desc_on
                else -> R.string.perf_desc_off
            }
        )
        perfProgress.isVisible = perfBusy

        // 授权状态
        setupState.setText(
            if (allPermissionsReady()) R.string.setup_entry_desc_ready
            else R.string.setup_entry_desc_pending
        )
    }

    private fun allPermissionsReady(): Boolean {
        val navOk = !NavModeManager.isSupported() || NavModeManager.canToggle(this)
        val wirelessOk = !WirelessDebug.isSupported() || WirelessDebug.canToggle(this)
        val perfOk = !PerfMode.isSupported(this) || PerfMode.canWriteAny(this)
        return navOk && wirelessOk && perfOk && ScreenTimeout.canWrite(this)
    }

    private fun onNavClick() {
        if (!NavModeManager.isSupported()) {
            snackbar(R.string.toast_nav_unsupported)
            return
        }
        if (!NavModeManager.canToggle(this)) {
            val needShizuku = NavModeManager.isHonorFamily() &&
                !ShizukuManager.hasPermission()
            showSetupSnackbar(
                if (needShizuku) R.string.toast_nav_need_shizuku
                else R.string.toast_nav_need_permission,
                navSetupSection()
            )
            return
        }
        if (navBusy) return

        navBusy = true
        navProgress.isVisible = true
        navButton.isEnabled = false

        Thread {
            val result = NavModeManager.switchMode(applicationContext)
            runOnUiThread {
                navBusy = false
                navProgress.isVisible = false
                refreshUi()
                updateWidget()
                requestTileRefresh(this)
                when (result) {
                    is SwitchResult.Success -> snackbar(
                        if (result.mode == NavMode.GESTURE) R.string.toast_nav_switched_gesture
                        else R.string.toast_nav_switched_three
                    )

                    is SwitchResult.Failed ->
                        snackbar(getString(R.string.toast_nav_failed, result.reason))

                    SwitchResult.NoPermission ->
                        showSetupSnackbar(R.string.toast_nav_need_permission, navSetupSection())

                    SwitchResult.NeedsShizuku ->
                        showSetupSnackbar(R.string.toast_nav_need_shizuku, SetupActivity.SECTION_SHIZUKU)

                    SwitchResult.Unsupported -> snackbar(R.string.toast_nav_unsupported)
                }
            }
        }.start()
    }

    private fun performWirelessToggle(target: Boolean) {
        if (!WirelessDebug.isSupported()) {
            refreshUi()
            snackbar(R.string.toast_wireless_unsupported)
            return
        }
        wirelessBusy = true
        pendingWirelessTarget = target
        refreshUi()

        WirelessDebug.setEnabled(this, target) { result ->
            if (isFinishing || isDestroyed) return@setEnabled
            wirelessBusy = false
            pendingWirelessTarget = null
            refreshUi()
            updateWidget()
            requestTileRefresh(this)
            when (result) {
                is WirelessDebug.Result.Success -> snackbar(
                    if (target) R.string.toast_wireless_enabled
                    else R.string.toast_wireless_disabled
                )

                WirelessDebug.Result.PermissionMissing, WirelessDebug.Result.ShizukuFailed ->
                    showSetupSnackbar(
                        R.string.toast_wireless_permission,
                        SetupActivity.SECTION_SHIZUKU
                    )

                WirelessDebug.Result.Failed ->
                    showSetupSnackbar(
                        R.string.toast_wireless_failed,
                        SetupActivity.SECTION_SHIZUKU
                    )

                WirelessDebug.Result.Unsupported ->
                    snackbar(R.string.toast_wireless_unsupported)
            }
        }
    }

    private fun performPerfToggle(target: Boolean) {
        if (!PerfMode.isSupported(this)) {
            refreshUi()
            snackbar(R.string.toast_perf_unsupported)
            return
        }
        if (!PerfMode.canWriteAny(this)) {
            refreshUi()
            showSetupSnackbar(R.string.toast_perf_permission, SetupActivity.SECTION_SHIZUKU)
            return
        }
        perfBusy = true
        pendingPerfTarget = target
        refreshUi()

        PerfMode.writeMode(this, target) { ok ->
            if (isFinishing || isDestroyed) return@writeMode
            perfBusy = false
            pendingPerfTarget = null
            refreshUi()
            updateWidget()
            requestTileRefresh(this)
            if (!ok) {
                showSetupSnackbar(R.string.toast_perf_failed, SetupActivity.SECTION_SHIZUKU)
            }
        }
    }

    private fun navSetupSection(): String =
        if (NavModeManager.isHonorFamily()) SetupActivity.SECTION_SHIZUKU
        else SetupActivity.SECTION_ADB

    private fun openSetup(section: String) {
        startActivity(
            Intent(this, SetupActivity::class.java)
                .putExtra(SetupActivity.EXTRA_SECTION, section)
        )
    }

    private fun showSetupSnackbar(messageRes: Int, section: String) {
        Snackbar.make(findViewById(R.id.root), messageRes, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_setup) { openSetup(section) }
            .show()
    }

    private fun snackbar(resId: Int) {
        Snackbar.make(findViewById(R.id.root), resId, Snackbar.LENGTH_SHORT).show()
    }

    private fun snackbar(text: CharSequence) {
        Snackbar.make(findViewById(R.id.root), text, Snackbar.LENGTH_LONG).show()
    }

    private fun updateWidget() {
        ToolboxWidgetProvider.updateAll(this)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_OPEN_TIMEOUT, false)) {
            intent.removeExtra(EXTRA_OPEN_TIMEOUT)
            startActivity(Intent(this, ScreenTimeoutActivity::class.java))
        }
        val section = intent.getStringExtra(EXTRA_SETUP_SECTION)
        if (section != null) {
            intent.removeExtra(EXTRA_SETUP_SECTION)
            openSetup(section)
        }
    }

    private fun appVersionName(): String = try {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun copyVersionInfo() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val info = "${getString(R.string.app_name)} v${appVersionName()} ($packageName)"
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), info))
        Toast.makeText(this, R.string.toast_version_copied, Toast.LENGTH_SHORT).show()
    }
}