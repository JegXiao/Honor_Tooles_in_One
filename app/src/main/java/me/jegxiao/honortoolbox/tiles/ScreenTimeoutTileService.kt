package me.jegxiao.honortoolbox.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import me.jegxiao.honortoolbox.R
import me.jegxiao.honortoolbox.ScreenTimeoutActivity
import me.jegxiao.honortoolbox.core.requestTileRefresh
import me.jegxiao.honortoolbox.core.startActivityFromTile
import me.jegxiao.honortoolbox.features.ScreenTimeout
import me.jegxiao.honortoolbox.widget.ToolboxWidgetProvider

/**
 * 控制中心磁贴：未激活写 2 分钟，激活写「永不熄屏」。
 */
class ScreenTimeoutTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!ScreenTimeout.canWrite(this)) {
            openPicker()
            return
        }
        toggleTimeout()
    }

    private fun toggleTimeout() {
        val current = ScreenTimeout.read(this)
        val enableNever = current != ScreenTimeout.NEVER_TIMEOUT_MILLIS
        val target = if (enableNever) {
            ScreenTimeout.NEVER_TIMEOUT_MILLIS
        } else {
            ScreenTimeout.SHORT_TIMEOUT_MILLIS
        }

        val success = ScreenTimeout.write(this, target)
        val message = when {
            success && enableNever -> getString(R.string.toast_timeout_never)
            success -> getString(R.string.toast_timeout_short)
            else -> getString(R.string.toast_timeout_failed)
        }
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        updateTile()
        requestTileRefresh(this)
        ToolboxWidgetProvider.updateAll(this)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val hasPermission = ScreenTimeout.canWrite(this)
        val current = ScreenTimeout.read(this)
        val isNever = current == ScreenTimeout.NEVER_TIMEOUT_MILLIS

        tile.label = getString(R.string.tile_timeout_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_screen_timeout)
        tile.state = if (hasPermission && isNever) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }

        val subtitle = when {
            !hasPermission -> getString(R.string.tile_timeout_need_permission)
            current == null -> getString(R.string.tile_timeout_unknown)
            isNever -> getString(R.string.tile_timeout_never)
            else -> getString(
                R.string.tile_timeout_current,
                ScreenTimeout.format(this, current)
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = subtitle
        }
        tile.contentDescription = getString(R.string.tile_timeout_desc, subtitle)
        tile.updateTile()
    }

    private fun openPicker() {
        startActivityFromTile(
            Intent(this, ScreenTimeoutActivity::class.java),
            REQUEST_CODE
        )
    }

    private companion object {
        const val REQUEST_CODE = 7_102
    }
}