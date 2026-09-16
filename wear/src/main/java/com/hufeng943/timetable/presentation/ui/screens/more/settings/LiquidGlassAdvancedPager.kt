package com.hufeng943.timetable.presentation.ui.screens.more.settings

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface

private data class AdjustTarget(val title: String, val value: Int, val range: IntRange, val suffix: String, val apply: (Int) -> Unit)

@Composable
fun LiquidGlassAdvancedPager(
    config: AppConfig,
    onEnabledChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onEffectChange: (com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onHighSaturationChange: (Boolean) -> Unit,
    onChromaticAberrationChange: (Boolean) -> Unit,
    onLensDistortionChange: (Float) -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
    onBlurredBackgroundChange: (Boolean) -> Unit,
    onBackgroundBlurRadiusChange: (Float) -> Unit,
) {
    var adjust by remember { mutableStateOf<AdjustTarget?>(null) }
    adjust?.let { target ->
        IntegerAdjustPager(target = target, onClose = { adjust = null })
        return
    }
    val state = rememberTransformingLazyColumnState()
    val transform = rememberTransformationSpec()
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    fun itemModifier(scope: androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope) =
        with(scope) { Modifier.fillMaxWidth().transformedHeight(this, transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding) }

    ScreenScaffold(scrollState = state) { padding ->
        TransformingLazyColumn(
            state = state,
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state),
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding
        ) {
            item { ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(stringResource(R.string.settings_liquid_glass_advanced))} }
            item { SwitchButton(checked=config.isLiquidGlassEnabled&&supported,onCheckedChange={if(supported)onEnabledChange(it)},enabled=supported,modifier=itemModifier(this),transformation=SurfaceTransformation(transform),icon={Icon(Icons.Rounded.BlurOn,null)},label={Text(stringResource(R.string.settings_liquid_glass))},secondaryLabel={Text(stringResource(if(supported)R.string.settings_liquid_glass_summary else R.string.settings_liquid_glass_unsupported))}) }
            item { OneUiCapsuleSurface(title=stringResource(R.string.settings_glass_opacity),subtitle="${(config.glassOpacity*100).toInt()}% · 点按精确调节",icon=Icons.Rounded.BlurOn,onClick={adjust=AdjustTarget("玻璃透明度",(config.glassOpacity*100).toInt(),5..95,"%") { onOpacityChange(it/100f) }},modifier=itemModifier(this)) }
            item { SwitchButton(checked=config.glassHighSaturation,onCheckedChange=onHighSaturationChange,modifier=itemModifier(this),transformation=SurfaceTransformation(transform),icon={Icon(Icons.Rounded.ColorLens,null)},label={Text("高饱和度色彩")},secondaryLabel={Text("增强玻璃后的色彩活力；关闭可进一步省电")}) }
            item { SwitchButton(checked=config.glassChromaticAberration,onCheckedChange=onChromaticAberrationChange,modifier=itemModifier(this),transformation=SurfaceTransformation(transform),icon={Icon(Icons.Rounded.ColorLens,null)},label={Text("色散效果")},secondaryLabel={Text("玻璃边缘 RGB 色散；滚动时关闭更流畅")}) }
            item { OneUiCapsuleSurface(title="镜头畸变",subtitle="${(config.glassLensDistortion*100).toInt()}% · 0% 可关闭折射",icon=Icons.Rounded.Tune,onClick={adjust=AdjustTarget("镜头畸变",(config.glassLensDistortion*100).toInt(),0..100,"%") { onLensDistortionChange(it/100f) }},modifier=itemModifier(this)) }
            item { SwitchButton(checked=config.glassBlurEnabled,onCheckedChange=onBlurEnabledChange,modifier=itemModifier(this),transformation=SurfaceTransformation(transform),icon={Icon(Icons.Rounded.BlurOn,null)},label={Text("模糊效果")},secondaryLabel={Text(if(config.glassBlurEnabled)"玻璃内模糊 ${config.glassBlurRadius.toInt()} dp" else "已关闭")}) }
            if(config.glassBlurEnabled)item { OneUiCapsuleSurface(title="玻璃模糊强度",subtitle="${config.glassBlurRadius.toInt()} dp · 点按精确调节",icon=Icons.Rounded.BlurOn,onClick={adjust=AdjustTarget("玻璃模糊强度",config.glassBlurRadius.toInt(),0..8," dp") { onBlurRadiusChange(it.toFloat()) }},modifier=itemModifier(this)) }
            item { SwitchButton(checked=config.blurredBackgroundEnabled,onCheckedChange=onBlurredBackgroundChange,modifier=itemModifier(this),transformation=SurfaceTransformation(transform),icon={Icon(Icons.Rounded.Wallpaper,null)},label={Text("显示模糊背景")},secondaryLabel={Text(if(config.blurredBackgroundEnabled)"全局背景模糊 ${config.backgroundBlurRadius.toInt()} dp（单层处理）" else "关闭时功耗更低")}) }
            if(config.blurredBackgroundEnabled)item { OneUiCapsuleSurface(title="背景模糊强度",subtitle="${config.backgroundBlurRadius.toInt()} dp · 点按精确调节",icon=Icons.Rounded.Wallpaper,onClick={adjust=AdjustTarget("背景模糊强度",config.backgroundBlurRadius.toInt(),0..12," dp") { onBackgroundBlurRadiusChange(it.toFloat()) }},modifier=itemModifier(this)) }
            item { OneUiCapsuleSurface(title=stringResource(R.string.settings_background_brightness),subtitle="${(config.backgroundBrightness*100).toInt()}% · 点按精确调节",icon=Icons.Rounded.Wallpaper,onClick={adjust=AdjustTarget("背景亮度",(config.backgroundBrightness*100).toInt(),10..100,"%") { onBrightnessChange(it/100f) }},modifier=itemModifier(this)) }
            item { OneUiCapsuleSurface(title=stringResource(R.string.settings_glass_readability),subtitle="背景亮度会同步调整对比度遮罩；高色散、高模糊仅建议用于重点玻璃卡片。",icon=Icons.Rounded.Wallpaper,modifier=itemModifier(this)) }
        }
    }
}

@Composable
private fun IntegerAdjustPager(target: AdjustTarget, onClose: () -> Unit) {
    var selected by remember(target.title) { mutableIntStateOf(target.value.coerceIn(target.range)) }
    val values = remember(target.range) { target.range.toList() }
    val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = values.indexOf(selected).coerceAtLeast(0) + 1)
    val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState=state,edgeButton={EdgeButton(onClick={target.apply(selected);onClose()}){Text("确定")}}){padding->
        TransformingLazyColumn(state=state,rotaryScrollableBehavior=RotaryScrollableDefaults.snapBehavior(state),modifier=Modifier.fillMaxSize(),contentPadding=padding){
            item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(target.title)}}
            items(values,key={it}){v->OneUiCapsuleSurface(title="$v${target.suffix}",selected=v==selected,onClick={selected=v},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
        }
    }
}
