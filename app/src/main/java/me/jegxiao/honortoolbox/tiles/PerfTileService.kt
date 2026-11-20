package me.jegxiao.honortoolbox.tiles

import android.content.Intent
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import me.jegxiao.honortoolbox.MainActivity
import me.jegxiao.honortoolbox.R
import me.jegxiao.honortoolbox.SetupActivity
import me.jegxiao.honortoolbox.core.requestTileRefresh
import me.jegxiao.honortoolbox.core.startActivityFromTile
import me.jegxiao.honortoolbox.features.PerfMode
import me.jegxiao.honortoolbox.widget.ToolboxWidgetProvider

/** 控制中心磁贴：一键切换性能模式 / 均衡模式。 */
class PerfTileService : TileService() {

    private var listening = false
    private var busy = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            updateTile()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        if (!listening) {
            contentResolver.registerContentObserver(PerfMode.settingsUri(), false, observer)
            listening = true
        }
        updateTile()
    }

    override fun onStopListening() {
        if (listening) {
            contentResolver.unregisterContentObserver(observer)
            listening = false
        }
        super.onStopListening()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (!PerfMode.isSupported(this)) {
            toast(R.string.toast_perf_unsupported)
            openApp()
            return
        }
        if (busy) return

        if (!PerfMode.canWriteAny(this)) {
            toast(R.string.toast_perf_permission)
            openSetup()
            return
        }

        busy = true
        PerfMode.writeMode(this, !PerfMode.isPerformance(this)) { ok ->
            busy = false
            if (!ok) toast(R.string.toast_perf_failed)
            updateTile()
            requestTileRefresh(this)
            ToolboxWidgetProvider.updateAll(this)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val mode = PerfMode.readMode(this)
        val supported = mode != null
        val on = mode == PerfMode.MODE_PERFORMANCE
        val ready = PerfMode.canWriteAny(this)

        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_perf_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_performance)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !supported -> getString(R.string.tile_perf_unsupported)
                !ready -> getString(R.string.tile_perf_need_permission)
                on -> getString(R.string.tile_perf_on)
                else -> getString(R.string.tile_perf_off)
            }
        }
        tile.updateTile()
    }

    private fun openApp() {
        startActivityFromTile(
            Intent(this, MainActivity::class.java),
            REQUEST_CODE
        )
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
        const val REQUEST_CODE = 7_104
    }
}