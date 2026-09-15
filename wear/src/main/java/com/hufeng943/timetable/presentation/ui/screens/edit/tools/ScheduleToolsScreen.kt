package com.hufeng943.timetable.presentation.ui.screens.edit.tools

import androidx.activity.compose.BackHandler
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
import com.hufeng943.timetable.presentation.ui.common.*
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.*
import kotlinx.datetime.*
import kotlin.time.Clock

enum class ScheduleToolPage { MAIN, TABLE, START_DATE, END_DATE, WINDOW_START, WINDOW_END, HOLIDAY_DAYS, OFFSET }

@Composable
fun ScheduleToolsScreen(viewModel: ScheduleToolsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val nav=LocalNavController.current; val config=LocalAppConfig.current
    LaunchedEffect(viewModel){viewModel.completed.collect{nav.popSafe()}}
    when(val current=state){
        ScheduleToolsState.Loading -> ScreenScaffold(timeText={}){}
        is ScheduleToolsState.Error -> SimpleMessageScreen("快捷修改",current.message)
        is ScheduleToolsState.Ready -> {
            val today=remember{Clock.System.todayIn(TimeZone.currentSystemDefault())}
            var tableId by remember{mutableLongStateOf(current.timetables.first().timetableId)}
            var courseIndex by remember { mutableIntStateOf(0) }
            var start by remember{mutableStateOf(today)}; var end by remember{mutableStateOf<LocalDate?>(null)}
            var offset by remember{mutableIntStateOf(10)}; var useWindow by remember{mutableStateOf(false)}
            var ws by remember{mutableStateOf(LocalTime(8,0))}; var we by remember{mutableStateOf(LocalTime(18,0))}
            var action by remember{mutableStateOf(BatchAction.SHIFT)}; var page by remember{mutableStateOf(ScheduleToolPage.MAIN)}
            val table=current.timetables.firstOrNull{it.timetableId==tableId}?:current.timetables.first()
            val selectedCourse = table.allCourses.getOrNull(courseIndex - 1)
            BackHandler(enabled=page!=ScheduleToolPage.MAIN){page=ScheduleToolPage.MAIN}
            when(page){
                ScheduleToolPage.TABLE -> TimetablePickerPage(current.timetables,tableId){tableId=it;courseIndex=0;page=ScheduleToolPage.MAIN}
                ScheduleToolPage.START_DATE -> DatePage(start){start=it;if(end!=null&&end!!<it)end=it;page=ScheduleToolPage.MAIN}
                ScheduleToolPage.END_DATE -> DatePage(end?:start){end=it;page=ScheduleToolPage.MAIN}
                ScheduleToolPage.WINDOW_START -> TimePage(ws,config.is24HourFormat){ws=it;page=ScheduleToolPage.MAIN}
                ScheduleToolPage.WINDOW_END -> TimePage(we,config.is24HourFormat){we=it;page=ScheduleToolPage.MAIN}
                ScheduleToolPage.HOLIDAY_DAYS -> ValuePickerPage("临时放假",(1..30).toList(),1,{"$it 天"}){days->
                    viewModel.apply(BatchAction.CANCEL,start,start.plus(days-1,DateTimeUnit.DAY),0,tableId,selectedCourse?.id,null,null)
                }
                ScheduleToolPage.OFFSET -> ValuePickerPage("统一提前 / 延时",(-120..120 step 5).filter{it!=0},offset,{if(it<0)"提前 ${-it} 分钟" else "延时 $it 分钟"}){offset=it;page=ScheduleToolPage.MAIN}
                ScheduleToolPage.MAIN -> {
                    val valid=(end==null||end!!>=start)&&(!useWindow||we>ws)&&(action!=BatchAction.SHIFT||offset!=0)
                    val scroll=rememberTransformingLazyColumnState();val transform=rememberTransformationSpec()
                    ScreenScaffold(scrollState=scroll,timeText={},edgeButton={EdgeButton(enabled=valid,onClick={viewModel.apply(action,start,end,offset,tableId,selectedCourse?.id,if(useWindow)ws else null,if(useWindow)we else null)}){Icon(Icons.Rounded.Check,"确认")}}){padding->
                        TransformingLazyColumn(state=scroll,contentPadding=padding,modifier=Modifier.fillMaxSize()){
                            item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text("快捷修改")}}
                            item{OneUiCapsuleSurface(title="课表：${table.semesterName}",subtitle="点按选择课表并查看课程/课时",onClick={page=ScheduleToolPage.TABLE},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="范围：${selectedCourse?.name ?: "全部课程"}",subtitle="点按循环选择全部/单门课程",onClick={courseIndex=(courseIndex+1)%(table.allCourses.size+1)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="临时放假",subtitle="从 ${start.toDisplayString()} 起 · 点按选择天数",icon=Icons.Rounded.EventBusy,emphasize=true,onClick={page=ScheduleToolPage.HOLIDAY_DAYS},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title=when(action){BatchAction.SHIFT->if(offset<0)"统一提前 ${-offset} 分钟" else "统一延时 $offset 分钟";BatchAction.CANCEL->"停课";BatchAction.RESTORE->"恢复正常"},subtitle="点按切换：调时 → 停课 → 恢复；长按设置提前/延时",icon=when(action){BatchAction.SHIFT->Icons.Rounded.Schedule;BatchAction.CANCEL->Icons.Rounded.EventBusy;BatchAction.RESTORE->Icons.Rounded.Restore},onClick={action=BatchAction.entries[(action.ordinal+1)%BatchAction.entries.size]},onLongClick={page=ScheduleToolPage.OFFSET},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            if(action==BatchAction.SHIFT)item{OneUiCapsuleSurface(title="提前 / 延时",subtitle=if(offset<0)"提前 ${-offset} 分钟" else "延时 $offset 分钟",icon=Icons.Rounded.Tune,onClick={page=ScheduleToolPage.OFFSET},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="开始：${start.toDisplayString()}",subtitle="点按选择生效日期",onClick={page=ScheduleToolPage.START_DATE},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title="结束：${end?.toDisplayString()?:"永不结束"}",subtitle="点按选择；长按设为永不结束",onClick={page=ScheduleToolPage.END_DATE},onLongClick={end=null},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            item{OneUiCapsuleSurface(title=if(useWindow)"仅处理 ${ws.toDisplayString(config.is24HourFormat)}–${we.toDisplayString(config.is24HourFormat)}" else "全部时段",subtitle="点按开关时间范围；长按设置开始",selected=useWindow,onClick={useWindow=!useWindow},onLongClick={useWindow=true;page=ScheduleToolPage.WINDOW_START},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                            if(useWindow)item{OneUiCapsuleSurface(title="时间窗结束：${we.toDisplayString(config.is24HourFormat)}",subtitle="点按修改",onClick={page=ScheduleToolPage.WINDOW_END},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                        }
                    }
                }
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
