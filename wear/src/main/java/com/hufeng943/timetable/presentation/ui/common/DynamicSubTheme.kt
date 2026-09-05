package com.hufeng943.timetable.presentation.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme
import com.hufeng943.timetable.presentation.ui.theme.LocalThemePreset
import com.hufeng943.timetable.presentation.ui.theme.ThemePreset
import com.materialkolor.PaletteStyle

/**
 * 局部动态色彩主题容器
 * 当传入的 [seedColor] 有效时，会自动基于该颜色生成专属的 Wear OS M3 调色板并应用到内部 Content 中；
 * 当颜色为 [Color.Unspecified] 时，则无缝回退并沿用系统/全局的当前的 [MaterialTheme]。
 */
@Composable
fun DynamicSubTheme(
    seedColor: Color,
    isDark: Boolean = true,  // Wear OS 默认使用深色主题
    isAmoled: Boolean = false,
    style: PaletteStyle = PaletteStyle.Vibrant,
    content: @Composable () -> Unit
) {
    // 只有当全局主题设置为 SYSTEM_DYNAMIC 时才应用动态子主题
    val useSeededSubTheme = LocalThemePreset.current == ThemePreset.SYSTEM_DYNAMIC

    if (useSeededSubTheme) {
        if (seedColor != Color.Unspecified) {
            val currentColorScheme = rememberDynamicColorScheme(
                seedColor = seedColor,
                isDark = isDark,
                isAmoled = isAmoled,
                style = style
            )
            MaterialTheme(colorScheme = currentColorScheme, content = content)
        } else {
            content()
        }
    } else {
        content()
    }
}
