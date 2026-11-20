package me.jegxiao.honortoolbox.core

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding

/**
 * 统一处理边到边显示：内容延伸到状态栏 / 导航栏下方，再通过 Insets 补回内边距，
 * 同时按深浅色设置系统栏图标明暗。
 */
fun Activity.applyEdgeToEdge(root: View) {
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // API 27 以下导航栏无法使用深色图标，给它一个深色背景。
    @Suppress("DEPRECATION")
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
        window.navigationBarColor = Color.BLACK
    }

    val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = !night
        isAppearanceLightNavigationBars = !night
    }

    val baseLeft = root.paddingLeft
    val baseTop = root.paddingTop
    val baseRight = root.paddingRight
    val baseBottom = root.paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.updatePadding(
            left = baseLeft + bars.left,
            top = baseTop + bars.top,
            right = baseRight + bars.right,
            bottom = baseBottom + bars.bottom
        )
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(root)
}