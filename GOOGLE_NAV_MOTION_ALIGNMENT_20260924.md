# Wear 导航动画 — Google 文档对齐修订（2026-09-24）

## 本次采用的效果

采用无水平位移的 cross-fade（交叉淡入淡出）：

- 前进：新页面 fade in，旧页面 fade out；
- 返回：上一页 fade in，当前页 fade out；
- 不使用任何横向 slide，避免圆屏上旧页面左移露边；
- 用户关闭 UI 动画或有效省电模式时，直接无动画切换；
- alpha 动画使用 Wear Material 3 `MaterialTheme.motionScheme.fastEffectsSpec<Float>()`，不硬编码时长。

Google 的 Navigation Compose Predictive Back 文档明确说明默认应用内返回为 cross-fade，并允许用 `popEnterTransition` / `popExitTransition` 自定义 fade、scale、slide。Wear Material 3 MotionScheme 文档明确将 Effects spec 用于 alpha/color 等不应 overshoot 的动画，因此这里使用 `fastEffectsSpec` 驱动 fade。

## 为什么这次没有直接重构到 Navigation 3

Google 当前 Wear OS 文档已经提供 Navigation 3 + `SwipeDismissableSceneStrategy`，这是长期迁移方向。但现有项目大量使用 Navigation 2 的字符串 route、nested graph、`getBackStackEntry(route)` 与 Hilt graph-scoped ViewModel。直接迁移会改变导航状态模型和 ViewModel scope，属于架构重构而非动画修复。

因此本次保留现有 Navigation 2 业务图，只将已存在的 Navigation Compose 宿主从“前进无动画”调整为 Google 文档一致的非空间 cross-fade，避免扩大回归面。

## 修改文件

- `wear/src/main/java/com/hufeng943/timetable/presentation/ui/AppNavHost.kt`

相对于用户本次上传的 root-fix 包，本轮仅修改这个文件的导航 transition 配置和对应注释；导出页等上一轮修复保持不变。

## 全工程源码检查

检查范围为本次上传压缩包解压后的完整工程：

- Kotlin 源文件：235 个；
- Kotlin/KTS 文件总数：240 个；
- XML：32 个；
- TOML：1 个；
- 结构化配置解析：0 错误；
- 235 个 Kotlin 文件词法级括号/字符串/注释结构扫描：0 错误；
- 文本源码/配置共 278 个文件：无 merge conflict 标记、无 NUL 字节；
- 黑色 Swipe scrim provider：0；
- 修改文件单独 `kotlinc` 解析：无 `expecting` / `unexpected tokens` / 缺失括号类解析错误。

## Gradle 编译验证

已执行：

`./gradlew :wear:compileDebugKotlin --stacktrace`

当前沙箱无法解析 `services.gradle.org`，Gradle Wrapper 9.4.1 无法下载，构建在进入项目配置/源码编译之前因 `UnknownHostException` 停止。因此不能宣称完整 Gradle 编译通过。建议在 Android Studio 或 CI 中再跑一次最终构建。
