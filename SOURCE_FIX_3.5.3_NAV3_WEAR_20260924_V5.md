# Timetable 3.5.3 — Wear Navigation 3 v5 修复与复审

日期：2026-09-24

## 1. CI 失败定位

上一版 `Timetable-3.5.3-wear-nav3-fix-v4.zip` 在 GitHub Actions 的 `:wear:compileDebugKotlin` 失败。日志明确指出 `AppNavHost.kt` 有以下 8 条直接编译错误：

- `281`：`NavKey` 没有 `route` 属性。
- `295`：`NavKey` 没有 `route` 属性。
- `297`：`NavKey` 没有 `route` 属性。
- `346`：`SwipeDismissableSceneStrategy<TimetableRouteKey>` 不能作为 `SceneStrategy<NavKey>` 使用。
- `549-551`：`navigateSingle` 扩展函数没有在 `AppNavHost.kt` 导入。

日志还显示 shared/mobile 测试与 Kotlin 编译均已执行，失败点集中在 Wear 模块。

## 2. v5 修改

### 导航泛型

将：

`rememberSwipeDismissableSceneStrategy<TimetableRouteKey>()`

改为：

`rememberSwipeDismissableSceneStrategy<NavKey>()`

与 Android 官方 Wear Navigation 3 示例保持一致。

### BackStack route 访问

`rememberNavBackStack()` 的状态类型是 `NavKey`，因此导航器不再直接读取 `lastOrNull()?.route` 或 `it.route`。现在统一先转换：

`(navBackStack.lastOrNull() as? TimetableRouteKey)?.route`

以及：

`(it as? TimetableRouteKey)?.route == target`

### navigateSingle

加入现有扩展函数：

`import com.hufeng943.timetable.presentation.ui.common.navigateSingle`

没有改变原有 Screen 层调用。

### 前进页面动画

为 `TimetableRouteKey` 的 `NavEntry` 添加 `NavDisplay.TransitionKey` 元数据，将前进导航覆盖为短时 cross-fade。

目的：消除用户截图中“上一页面向左移动、露出一部分、卡住后突然消失”的前进导航视觉路径。Wear 的 pop / predictive-back 元数据没有被覆盖，因此边缘返回能力仍交给 `SwipeDismissableSceneStrategy`。

## 3. Wear 渲染优化保留

继续保留上一版已经加入的：

- inactive destination 使用 `LocalScreenIsActive` 时关闭 Kyant backdrop shader。
- 全局 backdrop 只在液态玻璃真正开启时创建。
- 低内存 Wear 设备跳过高成本 blur/depth。
- 保留稳定 Hilt key，避免编辑页子页面重新创建编辑 ViewModel。

## 4. 静态复审

本次重新检查：

- AppNavHost `{}` 数量平衡。
- AppNavHost `()` 数量平衡。
- 不再存在 `rememberSwipeDismissableSceneStrategy<TimetableRouteKey>()`。
- 不再存在对 `navBackStack` 元素直接 `.route` 的访问。
- `navigateSingle` 已导入。
- `NavKey` / Navigation 3 metadata API 已导入。
- 所有旧 `SwipeDismissableNavHost` / `rememberSwipeDismissableNavController` 引用此前已清理。

## 5. 验证边界

没有在当前环境执行完整 Android/Gradle 构建；本项目以 GitHub Actions 为权威构建验证。下一次应重新执行与失败日志相同的任务：

`./gradlew --no-daemon :shared:test :mobile:testDebugUnitTest :wear:testDebugUnitTest`

然后再执行 Wear release 构建。

## 6. 外部依据

本次 Navigation 3 泛型与 metadata 处理依据 Android 官方当前 Wear Navigation 3 文档和 API：
- `rememberSwipeDismissableSceneStrategy<NavKey>()` 的官方迁移示例。
- `NavDisplay.TransitionKey` 可用于覆盖前进导航的 `ContentTransform`。
- Wear Navigation 3 的 `SwipeDismissableSceneStrategy` 专门负责 Wear 的 swipe-to-dismiss / predictive back。
