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
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.hufeng943.timetable.presentation.ui.common.*
import com.hufeng943.timetable.presentation.ui.common.ui.mappers.toCourseUi
import com.hufeng943.timetable.presentation.ui.components.OneUiCapsuleSurface
import com.hufeng943.timetable.presentation.ui.components.edit.EditCourseCard
import com.hufeng943.timetable.presentation.ui.components.edit.EditTimeSlotCard
import com.hufeng943.timetable.presentation.ui.components.toDisplayString
import com.hufeng943.timetable.presentation.viewmodel.edit.tools.*
import com.hufeng943.timetable.shared.model.Course
import com.hufeng943.timetable.shared.model.ResolvedSchedule
import kotlinx.datetime.*
import java.time.format.TextStyle
import kotlin.time.Clock

private object AdjustRoutes { const val MAIN="main"; const val DATE="date"; const val A_COURSES="a_courses"; const val A_SLOTS="a_slots"; const val B_COURSES="b_courses"; const val B_SLOTS="b_slots" }

@Composable
fun CourseAdjustmentScreen(viewModel: ScheduleAdjustmentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle(); val outerNav=LocalNavController.current
    LaunchedEffect(viewModel){viewModel.completed.collect{outerNav.popSafe()}}
    when(val current=state){
        ScheduleAdjustmentState.Loading -> ScreenScaffold(timeText={}){}
        is ScheduleAdjustmentState.Error -> SimpleMessageScreen("课程调节",current.message)
        is ScheduleAdjustmentState.Ready -> {
            val table=current.timetables.first(); val today=remember{Clock.System.todayIn(TimeZone.currentSystemDefault())}
            var date by remember{mutableStateOf(today)}; var sourceSlotId by remember{mutableLongStateOf(-1L)}; var targetCourseId by remember{mutableLongStateOf(-1L)}
            var browsingCourseId by remember{mutableLongStateOf(-1L)}; var mode by remember{mutableStateOf(CourseAdjustmentMode.OCCUPY)}; var permanent by remember{mutableStateOf(false)}
            val occurrences=remember(table,date){table.resolveDate(date)}
            val a=occurrences.firstOrNull{it.timeSlot.id==sourceSlotId}?:occurrences.firstOrNull().also{if(sourceSlotId<0&&it!=null)sourceSlotId=it.timeSlot.id}
            val bCourses=table.allCourses.filter{it.id!=a?.course?.id}; val b=bCourses.firstOrNull{it.id==targetCourseId}?:bCourses.firstOrNull().also{if(targetCourseId<0&&it!=null)targetCourseId=it.id}
            val bOccurs=b?.let{bc->occurrences.any{it.course.id==bc.id}}==true; val valid=a!=null&&b!=null&&(mode!=CourseAdjustmentMode.SWAP||bOccurs)
            val nav=rememberSwipeDismissableNavController()
            SwipeDismissableNavHost(navController=nav,startDestination=AdjustRoutes.MAIN){
                composable(AdjustRoutes.MAIN){
                    val scroll=rememberTransformingLazyColumnState();val transform=rememberTransformationSpec()
                    ScreenScaffold(scrollState=scroll,timeText={},edgeButton={EdgeButton(enabled=valid,onClick={if(a!=null&&b!=null)viewModel.applyCourseAdjustment(table.timetableId,date,a.timeSlot.id,b.id,mode,permanent)}){Icon(Icons.Rounded.Check,"确认")}}){padding->TransformingLazyColumn(state=scroll,contentPadding=padding,modifier=Modifier.fillMaxSize()){
                        item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text("课程调节")}}
                        item{OneUiCapsuleSurface(title="日期：${date.toDisplayString()}",subtitle="${date.dayOfWeek.toDisplayString(TextStyle.FULL)} · 点按打开日期选择",icon=Icons.Rounded.DateRange,onClick={nav.navigateSingle(AdjustRoutes.DATE)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                        item{OneUiCapsuleSurface(title="A 课：${a?.course?.name?:"当天无课"}",subtitle=a?.let{"${it.startTime.toDisplayString(true)}–${it.endTime.toDisplayString(true)} · 点按按课程→课时选择"}?:"请选择其他日期",onClick={nav.navigateSingle(AdjustRoutes.A_COURSES)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                        item{OneUiCapsuleSurface(title="B 课：${b?.name?:"无可选课程"}",subtitle="点按按课程→课时选择",onClick={nav.navigateSingle(AdjustRoutes.B_COURSES)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                        item{OneUiCapsuleSurface(title=if(mode==CourseAdjustmentMode.SWAP)"换课" else "占课",subtitle=if(mode==CourseAdjustmentMode.SWAP)if(bOccurs)"A、B 互换" else "B 当天无课时，不能换课" else "B 使用 A 的时间，A 当天取消",icon=Icons.Rounded.SwapCalls,selected=mode==CourseAdjustmentMode.SWAP,onClick={mode=if(mode==CourseAdjustmentMode.SWAP)CourseAdjustmentMode.OCCUPY else CourseAdjustmentMode.SWAP},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                        item{OneUiCapsuleSurface(title=if(permanent)"永久" else "仅当天",subtitle=if(permanent)"同步修改实际课时归属" else "只修改所选日期",icon=if(permanent)Icons.Rounded.Repeat else Icons.Rounded.Today,selected=permanent,onClick={permanent=!permanent},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                        item{OneUiCapsuleSurface(title="恢复课程调节",subtitle="恢复由调休、课程调节产生的修改",icon=Icons.Rounded.Restore,onClick={viewModel.restoreAdjustments(table.timetableId)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding))}
                    }}
                }
                composable(AdjustRoutes.DATE){ScreenScaffold(timeText={}){DatePicker(initialDate=date.toJavaLocalDate(),onDatePicked={date=it.toKotlinLocalDate();sourceSlotId=-1;targetCourseId=-1;nav.popSafe()})}}
                composable(AdjustRoutes.A_COURSES){
                    val courses=occurrences.map{it.course}.distinctBy{it.id}
                    CourseSelectionPage("选择 A 课",courses){browsingCourseId=it;nav.navigateSingle(AdjustRoutes.A_SLOTS)}
                }
                composable(AdjustRoutes.A_SLOTS){
                    val slots=occurrences.filter{it.course.id==browsingCourseId}
                    OccurrenceSelectionPage("选择 A 课时",slots){sourceSlotId=it;targetCourseId=-1;nav.popBackStack(AdjustRoutes.MAIN,false)}
                }
                composable(AdjustRoutes.B_COURSES){CourseSelectionPage("选择 B 课",bCourses){browsingCourseId=it;nav.navigateSingle(AdjustRoutes.B_SLOTS)}}
                composable(AdjustRoutes.B_SLOTS){
                    val course=bCourses.firstOrNull{it.id==browsingCourseId}
                    TimeSlotSelectionPage("选择 B 课时",course){targetCourseId=browsingCourseId;nav.popBackStack(AdjustRoutes.MAIN,false)}
                }
            }
        }
    }
}

@Composable private fun CourseSelectionPage(title:String,courses:List<Course>,onCourse:(Long)->Unit){val state=rememberTransformingLazyColumnState();val transform=rememberTransformationSpec();ScreenScaffold(scrollState=state,timeText={}){padding->TransformingLazyColumn(state=state,contentPadding=padding,modifier=Modifier.fillMaxSize()){item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(title)}};items(courses,key={it.id}){course->EditCourseCard(course=course.toCourseUi(),onClick={onCourse(course.id)},onLongClick={onCourse(course.id)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),transformation=SurfaceTransformation(transform))}}}}
@Composable private fun OccurrenceSelectionPage(title:String,items:List<ResolvedSchedule>,onSelect:(Long)->Unit){val state=rememberTransformingLazyColumnState();val transform=rememberTransformationSpec();ScreenScaffold(scrollState=state,timeText={}){padding->TransformingLazyColumn(state=state,contentPadding=padding,modifier=Modifier.fillMaxSize()){item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(title)}};items(items,key={it.timeSlot.id}){o->EditTimeSlotCard(timeSlot=o.course.toCourseUi(o.timeSlot).selectedTimeSlot!!,onClick={onSelect(o.timeSlot.id)},modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),transformation=SurfaceTransformation(transform))}}}}
@Composable private fun TimeSlotSelectionPage(title:String,course:Course?,onSelect:()->Unit){val state=rememberTransformingLazyColumnState();val transform=rememberTransformationSpec();ScreenScaffold(scrollState=state,timeText={}){padding->TransformingLazyColumn(state=state,contentPadding=padding,modifier=Modifier.fillMaxSize()){item{ListHeader(modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),transformation=SurfaceTransformation(transform)){Text(title)}};course?.toCourseUi()?.timeSlots?.let{slots->items(slots,key={it.id}){slot->EditTimeSlotCard(timeSlot=slot,onClick=onSelect,modifier=Modifier.fillMaxWidth().transformedHeight(this,transform).minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),transformation=SurfaceTransformation(transform))}}}}}
