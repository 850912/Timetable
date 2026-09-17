package com.hufeng943.timetable.presentation.ui.screens.more.settings

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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import androidx.compose.material.icons.rounded.Check
import com.hufeng943.timetable.R
import com.hufeng943.timetable.presentation.ui.common.AppConfig
import com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.OneUiSwitchCapsule

private data class AdjustTarget(val title: String, val value: Int, val range: IntRange, val suffix: String, val apply: (Int) -> Unit)

@Composable
fun LiquidGlassAdvancedPager(
    config: AppConfig,
    onEnabledChange: (Boolean) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onEffectChange: (com.hufeng943.timetable.presentation.ui.common.LiquidGlassEffect) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onChromaticAberrationChange: (Boolean) -> Unit,
    onLensDistortionChange: (Float) -> Unit,
    onBlurEnabledChange: (Boolean) -> Unit,
    onBlurRadiusChange: (Float) -> Unit,
) {
    val nav = rememberSwipeDismissableNavController()
    val internalBackStackEntry by nav.currentBackStackEntryAsState()
    val internalSwipeBackEnabled = internalBackStackEntry != null && nav.previousBackStackEntry != null
    var adjust by remember { mutableStateOf<AdjustTarget?>(null) }

    fun openAdjust(target: AdjustTarget) {
        adjust = target
        nav.navigateSingle("adjust")
    }

    SwipeDismissableNavHost(navController = nav, userSwipeEnabled = internalSwipeBackEnabled, startDestination = "main") {
        composable("main") {
            val state = rememberTransformingLazyColumnState()
            val transform = rememberTransformationSpec()
            @Composable
            fun itemModifier(scope: androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope): Modifier = with(scope) {
                Modifier.fillMaxWidth().transformedHeight(this, transform)
                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
            }
            ScreenScaffold(scrollState = state) { padding ->
                TransformingLazyColumn(state = state, rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(state), modifier = Modifier.fillMaxSize(), contentPadding = padding) {
                    item { ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(stringResource(R.string.settings_liquid_glass_advanced))} }
                    item { OneUiSwitchCapsule(title=stringResource(R.string.settings_liquid_glass),subtitle="光学折射玻璃",icon=Icons.Rounded.BlurOn,checked=config.isLiquidGlassEnabled,onCheckedChange=onEnabledChange,modifier=itemModifier(this)) }
                    if (!config.conditionalUiEnabled || config.isLiquidGlassEnabled) item { OneUiCapsuleSurface(
                        title = "效果：${when(config.liquidGlassEffect){LiquidGlassEffect.SOFT->"轻量";LiquidGlassEffect.BALANCED->"均衡";LiquidGlassEffect.FLUID->"增强"}}",
                        subtitle = "点按切换",
                        icon = Icons.Rounded.Tune,
                        onClick = { onEffectChange(LiquidGlassEffect.entries[(config.liquidGlassEffect.ordinal + 1) % LiquidGlassEffect.entries.size]) },
                        modifier = itemModifier(this)
                    ) }
                    if (!config.conditionalUiEnabled || config.isLiquidGlassEnabled) item { OneUiCapsuleSurface(title=stringResource(R.string.settings_glass_opacity),subtitle="${(config.glassOpacity*100).toInt()}% · 点按调节",icon=Icons.Rounded.BlurOn,onClick={openAdjust(AdjustTarget("玻璃透明度",(config.glassOpacity*100).toInt().coerceIn(10,70),10..70,"%") { onOpacityChange(it/100f) })},modifier=itemModifier(this)) }
                    if (!config.conditionalUiEnabled || (config.isLiquidGlassEnabled && config.liquidGlassEffect != LiquidGlassEffect.SOFT)) item { OneUiSwitchCapsule(title="色散效果",subtitle="均衡/增强档生效",icon=Icons.Rounded.ColorLens,checked=config.glassChromaticAberration,onCheckedChange=onChromaticAberrationChange,modifier=itemModifier(this)) }
                    if (!config.conditionalUiEnabled || config.isLiquidGlassEnabled) item { OneUiCapsuleSurface(title="折射强度",subtitle="${(config.glassLensDistortion*100).toInt()}%",icon=Icons.Rounded.Tune,onClick={openAdjust(AdjustTarget("折射强度",(config.glassLensDistortion*100).toInt().coerceIn(0,60),0..60,"%") { onLensDistortionChange(it/100f) })},modifier=itemModifier(this)) }
                    if (!config.conditionalUiEnabled || config.isLiquidGlassEnabled) item { OneUiSwitchCapsule(title="轻微模糊",subtitle=if(config.glassBlurEnabled)"${config.glassBlurRadius.toInt()} dp" else "已关闭",icon=Icons.Rounded.BlurOn,checked=config.glassBlurEnabled,onCheckedChange=onBlurEnabledChange,modifier=itemModifier(this)) }
                    if(config.glassBlurEnabled && (!config.conditionalUiEnabled || config.isLiquidGlassEnabled)) item { OneUiCapsuleSurface(title="模糊强度",subtitle="${config.glassBlurRadius.toInt()} dp",icon=Icons.Rounded.BlurOn,onClick={openAdjust(AdjustTarget("模糊强度",config.glassBlurRadius.toInt().coerceIn(0,2),0..2," dp") { onBlurRadiusChange(it.toFloat()) })},modifier=itemModifier(this)) }
                    item { OneUiCapsuleSurface(title=stringResource(R.string.settings_background_brightness),subtitle="${(config.backgroundBrightness*100).toInt()}% · 点按调节",icon=Icons.Rounded.Wallpaper,onClick={openAdjust(AdjustTarget("背景亮度",(config.backgroundBrightness*100).toInt(),10..100,"%") { onBrightnessChange(it/100f) })},modifier=itemModifier(this)) }
                    item { OneUiCapsuleSurface(title=stringResource(R.string.settings_glass_readability),subtitle="Wear 已优化玻璃性能。",icon=Icons.Rounded.Wallpaper,modifier=itemModifier(this)) }
                }
            }
        }
        composable("adjust") {
            val target = adjust
            if (target != null) IntegerAdjustPager(target) { nav.popBackStack() }
        }
    }
}

@Composable
private fun IntegerAdjustPager(target: AdjustTarget, onClose: () -> Unit) {
    var selected by remember(target.title) { mutableIntStateOf(target.value.coerceIn(target.range)) }
    val values = remember(target.range) { target.range.toList() }
    val state = rememberTransformingLazyColumnState(initialAnchorItemIndex = values.indexOf(selected).coerceAtLeast(0) + 1)
    val transform = rememberTransformationSpec()
    ScreenScaffold(scrollState=state,edgeButton={EdgeButton(onClick={target.apply(selected);onClose()}){Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.check))}}){padding->
        TransformingLazyColumn(state=state,rotaryScrollableBehavior=RotaryScrollableDefaults.snapBehavior(state),modifier=Modifier.fillMaxSize(),contentPadding=padding){
            item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(target.title)}}
            items(values,key={it}){v->OneUiCapsuleSurface(title="$v${target.suffix}",selected=v==selected,onClick={selected=v},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
        }
    }
}
