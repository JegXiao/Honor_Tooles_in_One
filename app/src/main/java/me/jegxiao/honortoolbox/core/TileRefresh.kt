package me.jegxiao.honortoolbox.core

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import me.jegxiao.honortoolbox.tiles.NavTileService
import me.jegxiao.honortoolbox.tiles.PerfTileService
import me.jegxiao.honortoolbox.tiles.ScreenTimeoutTileService
import me.jegxiao.honortoolbox.tiles.WirelessDebugTileService

/** 在应用内改了状态后，让四个控制中心磁贴重新读取并刷新。 */
fun requestTileRefresh(context: Context) {
    val appContext = context.applicationContext
    listOf(
        NavTileService::class.java,
        ScreenTimeoutTileService::class.java,
        WirelessDebugTileService::class.java,
        PerfTileService::class.java
    ).forEach { service ->
        runCatching {
            TileService.requestListeningState(
                appContext,
                ComponentName(appContext, service)
            )
        }
    }
}