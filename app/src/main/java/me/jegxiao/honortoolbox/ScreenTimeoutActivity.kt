package me.jegxiao.honortoolbox

import android.app.StatusBarManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import me.jegxiao.honortoolbox.core.applyEdgeToEdge
import me.jegxiao.honortoolbox.core.requestTileRefresh
import me.jegxiao.honortoolbox.features.ScreenTimeout
import me.jegxiao.honortoolbox.tiles.ScreenTimeoutTileService
import me.jegxiao.honortoolbox.widget.ToolboxWidgetProvider

class ScreenTimeoutActivity : AppCompatActivity() {

    private lateinit var currentTimeoutValue: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var grantPermissionButton: MaterialButton
    private lateinit var timeoutGroup: RadioGroup
    private lateinit var optionsPermissionHint: TextView

    private val options by lazy { ScreenTimeout.options(this) }
    private var bindingSelection = false

    private val timeoutObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_timeout)
        applyEdgeToEdge(findViewById(R.id.root))

        currentTimeoutValue = findViewById(R.id.current_timeout_value)
        permissionStatus = findViewById(R.id.permission_status)
        grantPermissionButton = findViewById(R.id.grant_permission_button)
        timeoutGroup = findViewById(R.id.timeout_group)
        optionsPermissionHint = findViewById(R.id.options_permission_hint)

        findViewById<View>(R.id.back_button).setOnClickListener { finish() }
        grantPermissionButton.setOnClickListener { openWriteSettingsPermission() }
        findViewById<MaterialButton>(R.id.add_tile_button).setOnClickListener {
            addQuickSettingsTile()
        }

        buildTimeoutOptions()
        timeoutGroup.setOnCheckedChangeListener { group, checkedId ->
            if (bindingSelection) return@setOnCheckedChangeListener

            val radio = group.findViewById<MaterialRadioButton>(checkedId)
                ?: return@setOnCheckedChangeListener
            val millis = radio.tag as? Int ?: return@setOnCheckedChangeListener

            if (!ScreenTimeout.canWrite(this)) {
                Toast.makeText(this, R.string.toast_timeout_permission, Toast.LENGTH_SHORT).show()
                openWriteSettingsPermission()
                refresh()
                return@setOnCheckedChangeListener
            }

            val success = ScreenTimeout.write(this, millis)
            if (success) {
                Toast.makeText(
                    this,
                    getString(
                        R.string.timeout_set_success,
                        ScreenTimeout.format(this, millis)
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                requestTileRefresh(this)
                ToolboxWidgetProvider.updateAll(this)
            } else {
                Toast.makeText(this, R.string.toast_timeout_failed, Toast.LENGTH_SHORT).show()
            }
            refresh()
        }

        refresh()
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(
            ScreenTimeout.settingUri(),
            false,
            timeoutObserver
        )
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(timeoutObserver)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildTimeoutOptions() {
        timeoutGroup.removeAllViews()
        options.forEach { option ->
            val radio = MaterialRadioButton(this).apply {
                id = View.generateViewId()
                text = option.label
                tag = option.millis
                textSize = 16f
                val verticalPadding = (12 * resources.displayMetrics.density).toInt()
                val horizontalPadding = (8 * resources.displayMetrics.density).toInt()
                setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
            }
            timeoutGroup.addView(radio)
        }
    }

    private fun refresh() {
        if (isFinishing || isDestroyed) return

        val current = ScreenTimeout.read(this)
        currentTimeoutValue.text = current
            ?.let { ScreenTimeout.format(this, it) }
            ?: getString(R.string.timeout_unknown)

        val canWrite = ScreenTimeout.canWrite(this)
        permissionStatus.setText(
            if (canWrite) R.string.timeout_permission_granted
            else R.string.timeout_permission_missing
        )
        grantPermissionButton.isVisible = !canWrite
        optionsPermissionHint.isVisible = !canWrite

        bindingSelection = true
        timeoutGroup.clearCheck()
        if (current != null) {
            for (index in 0 until timeoutGroup.childCount) {
                val radio = timeoutGroup.getChildAt(index) as? MaterialRadioButton
                    ?: continue
                if (radio.tag == current) {
                    radio.isChecked = true
                    break
                }
            }
        }
        bindingSelection = false

        if (canWrite) {
            requestTileRefresh(this)
        }
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
                // 尝试下一个系统设置入口。
            } catch (_: SecurityException) {
                // 部分定制系统会拦截，继续回退。
            }
        }

        Toast.makeText(this, R.string.write_settings_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun addQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val statusBarManager = getSystemService(StatusBarManager::class.java)
                statusBarManager.requestAddTileService(
                    ComponentName(this, ScreenTimeoutTileService::class.java),
                    getString(R.string.tile_timeout_label),
                    Icon.createWithResource(this, R.drawable.ic_screen_timeout),
                    mainExecutor
                ) {
                    Toast.makeText(
                        this,
                        R.string.timeout_add_tile_requested,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return
            } catch (_: Exception) {
                // 部分定制系统没有提供自动添加入口，回退到手动说明。
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.timeout_add_tile)
            .setMessage(R.string.timeout_add_tile_instructions)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}