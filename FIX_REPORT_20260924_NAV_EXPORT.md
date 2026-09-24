# Timetable 3.5.3 Wear — 导出布局与导航残影根因修复

日期：2026-09-24

## 1. 导出目标（手机）显示修复

### 根因
原实现把 `PHONE_APP / PHONE_FILE / BOTH` 三个完整 `OneUiCapsuleSurface` 横向塞进同一行。圆形手表常见的 192–220dp 宽度下，每项还包含内部左右 padding、图标、文字以及“仅在选中时才出现”的 25dp 勾选区域，因此：

- 三列本身没有足够的文本宽度；
- 选中后新增 trailing check，触发二次测量，标题/副标题会再次收缩；
- 视觉上表现为截断、挤压以及选中态布局跳动。

### 修复
`ExportScreen.kt`：

- 三列横排改为 3 个纵向全宽紧凑选择项；
- 移除目标项的大图标，把有限宽度留给文字；
- 新增 `ExportTargetOption`；
- 无论是否选中都固定预留 25dp trailing 槽；
- 只有选中时在固定槽内绘制勾选，不再改变正文可用宽度；
- 标题/副标题限制单行，保证小尺寸圆屏稳定。

这不是通过缩小字体硬塞，而是改变信息布局以适配手表屏幕。

## 2. “进入子页面时上一页左移、停住、突然消失”根因修复

### 实际根因
当前源码为了规避问题，将 Wear Compose Navigation 固定在 1.4.1，并使用默认黑色 swipe scrim/裁剪等方式隐藏转场边缘。

检查 AndroidX 官方源码后确认：Wear `SwipeDismissableNavHost` 新版在 API 36+ 的 `PredictiveBackNavHost` 中，**前进导航本身就定义了空间位移动画**：旧页面通过 `slideOutHorizontally(targetOffsetX = { -it / 2 })` 向左离场，新页面同时缩放/淡入。这与用户看到的“上一页往左漏出一部分”属于同一视觉机制。

因此：

- 单纯升级到 1.6.x 不能解决；
- 用黑色 scrim 覆盖只能隐藏现象；
- `clipToBounds` 只能裁切越界像素，也不改变转场机制。

### 修复策略
保留现有路由、nested graph、Hilt graph-scoped ViewModel 和业务导航调用，仅替换 app graph 的呈现宿主：

1. app graph 改用 `androidx.navigation.compose.NavHost`（项目现有版本 2.9.8）；
2. 所有**前进导航**统一为 `EnterTransition.None / ExitTransition.None`，彻底取消旧页向左平移；
3. 返回只允许非空间的 alpha 过渡，且跟随应用 `uiAnimationsEnabled`；
4. API 36+：由 Navigation Compose 接管平台 Predictive Back；
5. API 35 及以下：在 NavHost 外使用 AndroidX Wear Foundation 官方 `BasicSwipeToDismissBox` 补齐右滑返回；
6. swipe scrim 使用透明色，让真实的 AppBackground 留在底层，不再用黑色全屏层掩盖问题；
7. `BasicSwipeToDismissBox` 使用稳定 key，路由变化不会因此重建整个 NavHost。

这次修的是产生左移残影的转场链路，而不是给残影加遮罩。

## 3. 依赖调整

`wear/build.gradle.kts`：

- 移除 Wear 模块对 `androidx.wear.compose:compose-navigation` 的直接依赖；
- 显式加入项目已经在 version catalog 中声明的 `androidx.navigation:navigation-compose:2.9.8`；
- 保留 `androidx.wear.compose:compose-foundation`，用于 API <= 35 的 `BasicSwipeToDismissBox`。

同时删除 version catalog 中已不再使用的 Wear compose-navigation alias，避免后续误以为根导航仍依赖该宿主。

## 4. 受影响文件

- `gradle/libs.versions.toml`
- `wear/build.gradle.kts`
- `wear/src/main/java/com/hufeng943/timetable/presentation/ui/AppNavHost.kt`
- `wear/src/main/java/com/hufeng943/timetable/presentation/ui/NavRoutes.kt`
- `wear/src/main/java/com/hufeng943/timetable/presentation/ui/screens/more/settings/LiquidGlassAdvancedPager.kt`（仅更新说明注释）
- `wear/src/main/java/com/hufeng943/timetable/presentation/ui/screens/more/settings/export/ExportScreen.kt`

## 5. 本地检查结果

已完成：

- 修改前后目录逐文件差异检查；
- Kotlin 括号/花括号平衡检查；
- 使用 `kotlinc` 做解析级语法检查，未发现 `expecting` / `unexpected tokens` 类语法错误；
- `libs.versions.toml` 解析通过；
- Wear XML 全量解析通过；
- Wear 生产源码中 `androidx.wear.compose.navigation` 直接引用数为 0；
- 黑色 Swipe scrim provider 数为 0。

### Gradle 编译限制
执行了：

`./gradlew :wear:compileDebugKotlin --stacktrace`

但沙箱无法解析 `services.gradle.org`，Gradle Wrapper 9.4.1 无法下载，构建在进入项目配置/Kotlin 编译前即因 `UnknownHostException` 终止。因此不虚报“本地编译通过”。建议在 Android Studio 或现有 GitHub Actions 中执行最终编译与真机验证。

## 6. 建议真机重点回归

1. 设置 -> 导出：检查 3 个手机目标在 192/200/220dp 圆屏上的完整显示；
2. 连续切换 3 个导出目标：确认标题不跳动、不缩窄；
3. 首页 -> 设置 -> 任意子页：确认进入时旧页不再向左露边；
4. 设置 -> 液态玻璃 -> 数值子页：连续多级进入/返回；
5. 编辑课表/课程/课时各子页面：确认 graph-scoped 编辑状态不丢失；
6. Wear OS 6 / API 36+：验证系统右滑 Predictive Back；
7. API 35 或更低模拟器：验证 `BasicSwipeToDismissBox` 的右滑返回；
8. 自定义图片/流体背景下进入子页，确认没有黑色全屏遮罩闪现。
