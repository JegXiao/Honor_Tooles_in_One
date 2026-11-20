package me.jegxiao.honortoolbox.tiles

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import me.jegxiao.honortoolbox.R
import me.jegxiao.honortoolbox.SetupActivity
import me.jegxiao.honortoolbox.core.requestTileRefresh
import me.jegxiao.honortoolbox.core.startActivityFromTile
import me.jegxiao.honortoolbox.features.WirelessDebug
import me.jegxiao.honortoolbox.widget.ToolboxWidgetProvider

/** 控制中心磁贴：一键开关系统「无线调试」。 */
class WirelessDebugTileService : TileService() {

    private var busy = false

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (!WirelessDebug.isSupported()) {
            toast(R.string.toast_wireless_unsupported)
            return
        }
        if (busy) return

        val target = !WirelessDebug.isEnabled(this)
        busy = true
        setTileState(target)

        WirelessDebug.setEnabled(this, target) { result ->
            busy = false
            updateTile()
            requestTileRefresh(this)
            ToolboxWidgetProvider.updateAll(this)
            when (result) {
                is WirelessDebug.Result.Success -> toast(
                    if (target) R.string.toast_wireless_enabled
                    else R.string.toast_wireless_disabled
                )

                WirelessDebug.Result.PermissionMissing -> {
                    toast(R.string.toast_wireless_permission)
                    openSetup()
                }

                WirelessDebug.Result.ShizukuFailed -> {
                    toast(R.string.toast_wireless_failed)
                    openSetup()
                }

                WirelessDebug.Result.Failed -> {
                    toast(R.string.toast_wireless_failed)
                    openSetup()
                }

                WirelessDebug.Result.Unsupported ->
                    toast(R.string.toast_wireless_unsupported)
            }
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val supported = WirelessDebug.isSupported()
        val enabled = supported && WirelessDebug.isEnabled(this)

        tile.label = getString(R.string.tile_wireless_label)
        tile.state = when {
            !supported -> Tile.STATE_UNAVAILABLE
            enabled -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when {
                    !supported -> R.string.tile_wireless_unsupported
                    enabled -> R.string.tile_wireless_on
                    else -> R.string.tile_wireless_off
                }
            )
        }
        tile.updateTile()
    }

    /** 操作进行中先按目标状态显示，避免磁贴看起来没有响应。 */
    private fun setTileState(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    private fun openSetup() {
        startActivityFromTile(
            Intent(this, SetupActivity::class.java)
                .putExtra(SetupActivity.EXTRA_SECTION, SetupActivity.SECTION_SHIZUKU),
            REQUEST_CODE
        )
    }

    private fun toast(resId: Int) {
        Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val REQUEST_CODE = 7_103
    }
}