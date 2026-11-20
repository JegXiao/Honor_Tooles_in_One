package me.jegxiao.honortoolbox.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import me.jegxiao.honortoolbox.R
import me.jegxiao.honortoolbox.SetupActivity
import me.jegxiao.honortoolbox.core.ShizukuManager
import me.jegxiao.honortoolbox.core.startActivityFromTile
import me.jegxiao.honortoolbox.features.NavMode
import me.jegxiao.honortoolbox.features.NavModeManager
import me.jegxiao.honortoolbox.features.SwitchResult
import me.jegxiao.honortoolbox.widget.ToolboxWidgetProvider

/** 控制中心磁贴：一键切换三键 / 手势导航。 */
class NavTileService : TileService() {

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

        if (!NavModeManager.isSupported()) {
            toast(R.string.tile_nav_unsupported)
            return
        }
        if (busy) return

        if (!NavModeManager.canToggle(this)) {
            toast(
                if (NavModeManager.isHonorFamily()) R.string.tile_nav_need_shizuku
                else R.string.tile_nav_need_permission
            )
            openSetup()
            return
        }

        busy = true
        Thread {
            val result = NavModeManager.switchMode(applicationContext)
            Handler(Looper.getMainLooper()).post {
                busy = false
                updateTile()
                ToolboxWidgetProvider.updateAll(this)
                when (result) {
                    is SwitchResult.Success -> toast(
                        if (result.mode == NavMode.GESTURE) R.string.toast_nav_switched_gesture
                        else R.string.toast_nav_switched_three
                    )

                    is SwitchResult.Failed ->
                        toast(getString(R.string.toast_nav_failed, result.reason))

                    SwitchResult.NoPermission -> {
                        toast(R.string.tile_nav_need_permission)
                        openSetup()
                    }

                    SwitchResult.NeedsShizuku -> {
                        toast(R.string.tile_nav_need_shizuku)
                        openSetup()
                    }

                    SwitchResult.Unsupported -> toast(R.string.tile_nav_unsupported)
                }
            }
        }.start()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val supported = NavModeManager.isSupported()
        val canToggle = NavModeManager.canToggle(this)
        val mode = NavModeManager.currentMode(this)
        val gesture = mode == NavMode.GESTURE

        tile.state = if (canToggle && gesture) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_nav_label)
        tile.contentDescription = getString(R.string.tile_nav_desc)
        tile.icon = Icon.createWithResource(
            this,
            if (canToggle) {
                if (gesture) R.drawable.ic_nav_gesture else R.drawable.ic_nav_three
            } else {
                R.drawable.ic_nav_switch
            }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !supported -> getString(R.string.tile_nav_unsupported)
                !canToggle && NavModeManager.isHonorFamily() ->
                    getString(R.string.tile_nav_need_shizuku)
                !canToggle -> getString(R.string.tile_nav_need_permission)
                else -> getString(NavModeManager.modeNameRes(mode))
            }
        }
        tile.updateTile()
    }

    private fun openSetup() {
        val section = if (NavModeManager.isHonorFamily() ||
            ShizukuManager.state(this) != ShizukuManager.State.GRANTED
        ) {
            SetupActivity.SECTION_SHIZUKU
        } else {
            SetupActivity.SECTION_ADB
        }
        startActivityFromTile(
            Intent(this, SetupActivity::class.java)
                .putExtra(SetupActivity.EXTRA_SECTION, section),
            REQUEST_CODE
        )
    }

    private fun toast(resId: Int) {
        Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).show()
    }

    private fun toast(text: CharSequence) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val REQUEST_CODE = 7_101
    }
}