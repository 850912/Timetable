package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hufeng943.timetable.presentation.ui.common.*
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.*
import kotlinx.datetime.*
import java.time.format.TextStyle
import kotlin.time.Clock

private object DayRoutes { const val MAIN="main"; const val DATE="date"; const val SOURCE="source" }

@Composable
fun DayArrangementScreen(viewModel: ScheduleAdjustmentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val outerNav=LocalNavController.current
    LaunchedEffect(viewModel){viewModel.completed.collect{outerNav.popSafe()}}
    when(val current=state){
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText={}){}
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("调休",current.message)
        is ScheduleAdjustmentState.Ready -> {
            val today=remember{Clock.System.todayIn(TimeZone.currentSystemDefault())}
            var date by remember{mutableStateOf(today)}; var sourceDay by remember{mutableStateOf(nextDifferentDay(today.dayOfWeek))}
            val nav=rememberNavController()
            NavHost(navController=nav,startDestination=DayRoutes.MAIN, enterTransition={androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(140))}, exitTransition={androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100))}, popEnterTransition={androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(140))}, popExitTransition={androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(100))}) {
                composable(DayRoutes.MAIN){
                    val scroll=rememberTransformingLazyColumnState();val transform=rememberTransformationSpec()
                    ScreenScaffold(scrollState=scroll,timeText={},edgeButton={EdgeButton(enabled=sourceDay!=date.dayOfWeek,onClick={viewModel.applyDayArrangement(null,date,sourceDay)}){Icon(Icons.Rounded.Check,"确认")}}){padding->
                        TransformingLazyColumn(state=scroll,contentPadding=padding,modifier=Modifier.fillMaxSize()){
                            item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text("调休")}}
                            item{OneUiCapsuleSurface(title="范围：全部课表",subtitle="${current.timetables.size} 个课表 · 全部课程/课时",icon=Icons.Rounded.SelectAll,modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="日期：${date.toDisplayString()}",subtitle="${date.dayOfWeek.toDisplayString(TextStyle.FULL)} · 点按选日期",icon=Icons.Rounded.DateRange,onClick={nav.navigateSingle(DayRoutes.DATE)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="改上：${sourceDay.toDisplayString(TextStyle.FULL)}的课",subtitle="选择来源星期",icon=Icons.Rounded.SwapHoriz,onClick={nav.navigateSingle(DayRoutes.SOURCE)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="恢复调休/换课",subtitle="恢复调休与课程调节",icon=Icons.Rounded.Restore,onClick={viewModel.restoreAdjustments()},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                        }
                    }
                }
                composable(DayRoutes.DATE){ScreenScaffold(timeText={}){DatePicker(initialDate=date.toJavaLocalDate(),onDatePicked={date=it.toKotlinLocalDate();if(sourceDay==date.dayOfWeek)sourceDay=nextDifferentDay(sourceDay);nav.popSafe()})}}
                composable(DayRoutes.SOURCE){DayOfWeekPicker(sourceDay,date.dayOfWeek){sourceDay=it;nav.popSafe()}}
            }
        }
    }
}

@Composable private fun DayOfWeekPicker(initial:DayOfWeek,excluded:DayOfWeek,onSelect:(DayOfWeek)->Unit){val state=rememberTransformingLazyColumnState();val transform=rememberTransformationSpec();ScreenScaffold(scrollState=state,timeText={}){padding->TransformingLazyColumn(state=state,contentPadding=padding,modifier=Modifier.fillMaxSize()){item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text("选择来源星期")}};DayOfWeek.entries.filter{it!=excluded}.forEach{day->item(key=day){OneUiCapsuleSurface(title=day.toDisplayString(TextStyle.FULL),selected=day==initial,onClick={onSelect(day)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}}}}}
private fun nextDifferentDay(day:DayOfWeek)=DayOfWeek.entries[(day.ordinal+1)%7]
