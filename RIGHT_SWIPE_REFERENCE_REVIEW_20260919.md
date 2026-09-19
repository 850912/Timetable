# Wear OS 右划返回参考复核

参考：Timetable-3.4.0-wear-performance-effects
目标：Timetable 3.5.0

## 结论

3.5.0 的内部导航现在与 3.4.0 已验证过的关键返回模型一致，并将重复逻辑集中到 `WearInternalNavHost`：

- 子流程使用 `rememberSwipeDismissableNavController()`。
- 子流程使用 Wear `SwipeDismissableNavHost` / Wear `composable`。
- 仅当 `previousBackStackEntry != null` 时启用内部 `userSwipeEnabled`。
- 子流程位于根 destination 时不抢占边缘右划，返回所有权交给父级 Wear NavHost。
- 未发现 `BackHandler`、`PredictiveBackHandler` 或普通 `androidx.navigation.compose.NavHost` 抢占 Wear 返回。
- Manifest 保持 `android:enableOnBackInvokedCallback="true"`。

## 已检查的嵌套流程

- SettingScreen
- LiquidGlassAdvancedPager
- EditTimetableScreen
- EditTimeSlotScreen
- ScheduleToolsScreen
- CourseAdjustmentScreen
- DayArrangementScreen

这些流程均使用统一的 `WearInternalNavHost`，因此行为应为：

`内部详情 -> 内部根页 -> 父页面 -> App 根页 -> 系统/表盘`

而不是在内部详情页一次右划就 pop 父页面。

## 与 3.4.0 的差异

3.4.0 在每个页面重复计算：

`currentBackStackEntry != null && previousBackStackEntry != null`

3.5.0 将相同规则集中进 `WearInternalNavHost`，减少以后某个子页面漏配 `userSwipeEnabled` 的风险。行为规则保持一致。

## 静态扫描

Wear 主源码中未发现实际使用：

- `androidx.navigation.compose.NavHost`
- `androidx.navigation.compose.composable`
- `BackHandler`
- `PredictiveBackHandler`
- `edgeSwipeToDismiss`
- `rememberNavController`

（注释中的 BackHandler/PredictiveBackHandler 字样不属于代码调用。）

## 编译验证限制

尝试执行 `./gradlew :wear:compileDebugKotlin --offline --no-daemon`，但 Gradle Wrapper 9.4.1 在当前环境仍尝试获取 `services.gradle.org`，DNS 不可用，因此无法完成真实 Kotlin/Gradle 编译。

建议实机最终验证四条路径：

1. 主页 -> 设置 -> UI 管理：右划应回设置，而非主页/退出 App。
2. 设置 -> UI 管理 -> 液态玻璃高级子页：逐级右划，每次只退一级。
3. 子页面右划到一半后取消：当前页面应回弹，back stack 不应 pop。
4. 回到 App 根页后右划：由系统执行 back-to-home / 表盘动画。
