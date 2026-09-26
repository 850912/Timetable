# Wear 返回动画审查（2026-09-17）

## 结论

返回动画异常的主要结构性风险来自“嵌套 SwipeDismissableNavHost”。外层 AppNavHost 已负责整个应用的返回/滑动关闭动画，但设置、课时编辑、课表编辑、批量调节、课程调节、调休、液态玻璃高级设置内部又各自创建了 SwipeDismissableNavHost。

当内部导航已经位于自己的 startDestination 时，它已经没有内部上一页，却仍默认启用 userSwipeEnabled。这样内部 Host 仍会参与返回手势竞争，容易让外层 Host 的返回手势/背景层动画表现不一致。

## 修复

对所有嵌套 SwipeDismissableNavHost：

- 使用 currentBackStackEntryAsState() 观察内部返回栈。
- 只有 previousBackStackEntry != null 时才启用内部 userSwipeEnabled。
- 内部子页面存在上一页：由内部 Host 负责标准 Wear swipe-to-dismiss 返回动画。
- 内部已经回到首页：内部 Host 不再抢手势，交还给外层 AppNavHost。
- 外层 AppNavHost 仍保持 swipe-to-dismiss，用于返回整个功能页。
- 保留 Wear Compose Navigation 1.6.2，不自制返回动画。

涉及文件：
- SettingScreen.kt
- LiquidGlassAdvancedPager.kt
- EditTimeSlotScreen.kt
- EditTimetableScreen.kt
- ScheduleToolsScreen.kt
- CourseAdjustmentScreen.kt
- DayArrangementScreen.kt

## 依赖/官方行为核对

项目使用 androidx.wear.compose 1.6.2。SwipeDismissableNavHost 官方支持 userSwipeEnabled，并在 API 36+ 使用平台 predictive back；API 35 及以下使用 BasicSwipeToDismissBox。返回动画应交由该组件处理，而不是再叠加 AnimatedContent/手写 slide 动画。

## 验证限制

尝试运行 :wear:compileDebugKotlin，但当前沙箱无法解析 services.gradle.org，Gradle Wrapper 无法下载 Gradle 9.4.1，因此未声称本地编译通过。请交给 GitHub Actions 做最终编译验证。
