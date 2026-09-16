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
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hufeng943.timetable.presentation.ui.common.LocalAppConfig
import com.hufeng943.timetable.presentation.ui.common.LocalNavController
import com.hufeng943.timetable.presentation.ui.common.navigateSingle
import com.hufeng943.timetable.presentation.ui.common.popSafe
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.*
import kotlinx.datetime.*
import kotlin.time.Clock

private object QuickRoutes { const val MAIN="main"; const val START="start"; const val END="end"; const val WS="ws"; const val WE="we"; const val DAYS="days"; const val OFFSET="offset" }

@Composable
fun ScheduleToolsScreen(
    fixedTimetableId: Long? = null,
    onCompleted: (() -> Unit)? = null,
    viewModel: ScheduleToolsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val outerNav = LocalNavController.current
    val config = LocalAppConfig.current
    LaunchedEffect(viewModel, onCompleted) { viewModel.completed.collect { onCompleted?.invoke() ?: outerNav.popSafe() } }
    when (val current = state) {
        ScheduleToolsState.Loading -> ScreenScaffold(timeText={}) {}
        is ScheduleToolsState.Error -> SimpleMessageScreen("快捷修改", current.message)
        is ScheduleToolsState.Ready -> {
            var tableIndex by remember { mutableIntStateOf(0) }
            LaunchedEffect(current.timetables.size) {
                if (tableIndex !in current.timetables.indices) tableIndex = 0
            }
            val table = if (fixedTimetableId != null) {
                current.timetables.firstOrNull { it.timetableId == fixedTimetableId }
            } else current.timetables.getOrNull(tableIndex)
            if (table == null) { SimpleMessageScreen("批量日程工具", "课表不存在"); return }
            val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
            var courseIndex by remember(table.timetableId) { mutableIntStateOf(0) }
            LaunchedEffect(table.timetableId, table.allCourses.size) {
                if (courseIndex !in 0..table.allCourses.size) courseIndex = 0
            }
            var start by remember { mutableStateOf(today) }; var end by remember { mutableStateOf<LocalDate?>(null) }
            var offset by remember { mutableIntStateOf(10) }; var useWindow by remember { mutableStateOf(false) }
            var ws by remember { mutableStateOf(LocalTime(8,0)) }; var we by remember { mutableStateOf(LocalTime(18,0)) }
            var action by remember { mutableStateOf(BatchAction.SHIFT) }
            val selectedCourse = table.allCourses.getOrNull(courseIndex - 1)
            val nav = rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(navController = nav, startDestination = QuickRoutes.MAIN) {
                composable(QuickRoutes.MAIN) {
                    val valid=(end?.let { it >= start } ?: true)&&(!useWindow||we>ws)&&(action!=BatchAction.SHIFT||offset!=0)
                    val scroll=rememberTransformingLazyColumnState(); val transform=rememberTransformationSpec()
                    ScreenScaffold(scrollState=scroll,timeText={},edgeButton={EdgeButton(enabled=valid,onClick={viewModel.apply(action,start,end,offset,table.timetableId,selectedCourse?.id,if(useWindow)ws else null,if(useWindow)we else null)}){Icon(Icons.Rounded.Check,"确认")}}){padding->
                        TransformingLazyColumn(state=scroll,contentPadding=padding,modifier=Modifier.fillMaxSize()){
                            item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(if(fixedTimetableId==null)"批量日程工具" else "快捷修改")}}
                            if(fixedTimetableId==null)item{OneUiCapsuleSurface(title="课表：${table.semesterName}",subtitle="点按切换课表",onClick={tableIndex=(tableIndex+1)%current.timetables.size},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="范围：${selectedCourse?.name ?: "全部课程"}",subtitle="点按循环选择全部/单门课程",onClick={courseIndex=(courseIndex+1)%(table.allCourses.size+1)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="临时放假",subtitle="从 ${start.toDisplayString()} 起 · 点按选择天数",icon=Icons.Rounded.EventBusy,emphasize=true,onClick={nav.navigateSingle(QuickRoutes.DAYS)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title=when(action){BatchAction.SHIFT->if(offset<0)"统一提前 ${-offset} 分钟" else "统一延时 $offset 分钟";BatchAction.CANCEL->"停课";BatchAction.RESTORE->"恢复正常"},subtitle="点按切换：调时 → 停课 → 恢复；长按设置提前/延时",icon=when(action){BatchAction.SHIFT->Icons.Rounded.Schedule;BatchAction.CANCEL->Icons.Rounded.EventBusy;BatchAction.RESTORE->Icons.Rounded.Restore},onClick={action=BatchAction.entries[(action.ordinal+1)%BatchAction.entries.size]},onLongClick={nav.navigateSingle(QuickRoutes.OFFSET)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            if(action==BatchAction.SHIFT)item{OneUiCapsuleSurface(title="提前 / 延时",subtitle=if(offset<0)"提前 ${-offset} 分钟" else "延时 $offset 分钟",icon=Icons.Rounded.Tune,onClick={nav.navigateSingle(QuickRoutes.OFFSET)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="开始：${start.toDisplayString()}",subtitle="点按选择生效日期",onClick={nav.navigateSingle(QuickRoutes.START)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="结束：${end?.toDisplayString()?:"永不结束"}",subtitle="点按选择；长按设为永不结束",onClick={nav.navigateSingle(QuickRoutes.END)},onLongClick={end=null},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title=if(useWindow)"仅处理 ${ws.toDisplayString(config.is24HourFormat)}–${we.toDisplayString(config.is24HourFormat)}" else "全部时段",subtitle="点按开关时间范围；长按设置开始",selected=useWindow,onClick={useWindow=!useWindow},onLongClick={useWindow=true;nav.navigateSingle(QuickRoutes.WS)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            if(useWindow)item{OneUiCapsuleSurface(title="时间窗结束：${we.toDisplayString(config.is24HourFormat)}",subtitle="点按修改",onClick={nav.navigateSingle(QuickRoutes.WE)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                        }
                    }
                }
                composable(QuickRoutes.START){ DatePage(start){selected->start=selected;if(end?.let { it < selected } == true)end=selected;nav.popSafe()} }
                composable(QuickRoutes.END){ DatePage(end?:start){end=it;nav.popSafe()} }
                composable(QuickRoutes.WS){ TimePage(ws,config.is24HourFormat){ws=it;nav.popSafe()} }
                composable(QuickRoutes.WE){ TimePage(we,config.is24HourFormat){we=it;nav.popSafe()} }
                composable(QuickRoutes.DAYS){ ValuePickerPage("临时放假",(1..30).toList(),1,{"$it 天"}){days->viewModel.apply(BatchAction.CANCEL,start,start.plus(days-1,DateTimeUnit.DAY),0,table.timetableId,selectedCourse?.id,null,null)} }
                composable(QuickRoutes.OFFSET){ ValuePickerPage("统一提前 / 延时",(-30..30).filter{it!=0},offset.coerceIn(-30,30),{if(it<0)"提前 ${-it} 分钟" else "延时 $it 分钟"}){offset=it;nav.popSafe()} }
            }
        }
    }
}

@Composable private fun ValuePickerPage(title:String,values:List<Int>,initial:Int,label:(Int)->String,onConfirm:(Int)->Unit){
    var selected by remember(initial){mutableIntStateOf(initial)};val state=rememberTransformingLazyColumnState(initialAnchorItemIndex=(values.indexOf(initial).coerceAtLeast(0)+1));val transform=rememberTransformationSpec()
    ScreenScaffold(scrollState=state,timeText={},edgeButton={EdgeButton(onClick={onConfirm(selected)}){Icon(Icons.Rounded.Check,"确认")}}){padding->TransformingLazyColumn(state=state,contentPadding=padding,modifier=Modifier.fillMaxSize()){
        item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(title)}}
        values.forEach{v->item(key=v){OneUiCapsuleSurface(title=label(v),selected=v==selected,onClick={selected=v},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}}
    }}
}
@Composable private fun DatePage(initial:LocalDate,onPicked:(LocalDate)->Unit){ScreenScaffold(timeText={}){DatePicker(initialDate=initial.toJavaLocalDate(),onDatePicked={onPicked(it.toKotlinLocalDate())})}}
@Composable private fun TimePage(initial:LocalTime,is24:Boolean,onPicked:(LocalTime)->Unit){ScreenScaffold(timeText={}){TimePicker(initialTime=initial.toJavaLocalTime(),onTimePicked={onPicked(it.toKotlinLocalTime())},timePickerType=if(is24)TimePickerType.HoursMinutes24H else TimePickerType.HoursMinutesAmPm12H)}}
