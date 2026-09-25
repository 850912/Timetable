# Timetable 3.4.0 全源码复审：Wear UI / 背景 / 玻璃 / 全局日程工具

日期：2026-09-17

## 审查范围

本轮对源码树进行了静态全量扫描，覆盖 `mobile/`、`shared/`、`wear/`：203 个 Kotlin 源文件、32 个 XML，以及 Gradle/资源配置。重点逐文件复核了 Wear Compose 的 AppScaffold/ScreenScaffold、背景绘制链、所有复用 One UI 容器、液态玻璃设置、调休/课程调节/批量日程工具与 Room 写入路径。

额外校验：32 个 XML 全部可解析；改动文件经 Kotlin 编译器做语法层扫描，没有 parser-level syntax error；源码无冲突标记。

完整 Gradle 编译未能在本环境执行：`./gradlew` 需要下载 Gradle 9.4.1，但执行环境无法连接 `services.gradle.org`。因此这里不声称已经完成 Android/Gradle 全量编译验证。

## 已修复：截图中的 UI 显示问题

1. **选中项被高亮层冲成大块浅色、文字对比度下降**：统一修改 `OneUiCapsuleSurface`。选中状态不再叠加大面积 Galaxy AI 光晕，改为暗色容器 + 主题色描边 + 轻量勾选标记。动态色、主题背景和图片背景下都保持文字对比度。
2. **删除确认页出现大块纯红区域**：破坏性卡片不再使用高强度红色容器，统一改成暗色容器 + 红色描边/图标/标题，避免圆屏上下边缘只剩纯红色块。
3. **圆屏卡片过高、长副标题挤占可视区**：统一限制复用胶囊的标题最多 2 行、副标题最多 3 行；普通导航胶囊副标题最多 3 行并使用省略号。
4. **编辑状态短暂黑底闪屏**：`HandleEditUiState` 的延迟加载占位层改为透明，不再覆盖全局背景。

## 已修复：流体背景/图片背景在未开启液态玻璃时不显示

根因在顶层绘制顺序：Wear Material3 `AppScaffold` 默认会使用主题 `background` 作为容器色，之前它会盖住独立绘制的 `AppBackground`。现已把 `AppScaffold.containerColor` 设置为 `Color.Transparent`，背景层无论是否开启液态玻璃都会正常可见。

`AppBackground` 本身仍负责：纯色、主题静态流体光晕、自定义图片、背景亮度遮罩，以及可选的单层背景模糊。图片丢失/无法解码时继续回退到主题光晕，避免黑屏。

## 已修复：液态玻璃在手表上的性能

保留项目已使用的 `Kyant0/AndroidLiquidGlass Backdrop 2.0.1`，避免再引入第二套模糊/折射框架。所有复用 One UI 表面仍走统一入口，但效果参数对 Wear OS 做了明显降级：

- 默认高饱和关闭；默认镜头畸变由 55% 降到 20%；默认玻璃模糊由 2dp 降到 1dp。
- 新增“轻量 / 均衡 / 增强”性能档位，并真正接入 `LiquidGlassEffect`；此前该配置存在但 UI 没有入口，渲染也没有按档位执行。
- 模糊渲染上限约 0.75 / 1.25 / 1.75dp；轻量档关闭折射；均衡/增强仅使用很小的折射量。
- RGB 色散只在增强档生效，避免滚动时重复承担最昂贵的视觉效果。
- 高光、阴影、内阴影强度下调；不增加随滚动变化的 shader 动画参数。

## 新增：全局磨砂玻璃开关

设置页新增 **“全局磨砂玻璃”** 开关。它可独立于液态玻璃开启，并复用现有 Backdrop：仅做轻量模糊 + 半透明表面，不做镜头折射/色散。

该设置通过 `AppConfig -> PreferenceStorage(DataStore) -> AppConfigViewModel -> LocalAppConfig` 全链路持久化，并由顶层统一创建 Backdrop；复用的 One UI 胶囊、信息胶囊、WatchCard、文本/颜色编辑容器、导出面板等都会使用同一套全局效果。

## 已修复：调休 / 换课 / 批量日程工具范围

### 调休

此前 UI 直接使用 `current.timetables.first()`，实际只处理第一个课表。现在调休默认以 `null timetableId` 进入 ViewModel，遍历所有课表；界面明确显示“全部课表 · 全部课程 · 全部课时”。恢复调休/换课也会遍历全部课表的 Room undo journal。

### 课程调节 / 换课

此前同样强制使用第一个课表。现在可在所有课表间直接切换，因此任意课表中的课程/课时都可作为 A/B 目标；切换课表时 A/B 选择会重置，避免把其他课表的 ID 带入当前课表。恢复课程调节改为恢复所有课表。

换课本身仍是“选定 A 课时与 B 课程后执行”的语义，没有按课程名称去猜测并批量修改其他课表中的同名课程，以避免误改不同学期/不同课表中的同名课程。

### 批量日程工具

此前可以切换到单一课表/单门课程。现在范围固定为所有课表、所有课程、所有课时，UI 不再提供会产生歧义的单课表/单课程范围选项。

更重要的是，全局批处理现在先为所有课表生成 `TimeSlotMutation`，最后一次调用 `repository.applyTimeSlotMutations()`，由 Room 在一个事务中提交。这样不会出现前几个课表已修改、后一个课表失败后留下半完成全局操作的情况。

## 统一性检查

- 顶层背景只有一个来源，ScreenScaffold 不再额外铺不透明背景。
- 复用卡片的选中、强调、破坏性状态统一由 `OneUiCapsuleSurface` 控制。
- 液态玻璃与磨砂玻璃共用同一 Backdrop/CompositionLocal，不再由不同页面各自创建 GPU 效果。
- 背景模糊保持单个全屏 blur 层，而不是每张卡片重复模糊整个背景。
- 批量操作 UI 文案与 ViewModel 实际作用范围一致。

## 联网核对依据

本轮实现前核对了最新官方/上游资料，而不是凭记忆改 API：

- Android Developers：Wear Material3 `AppScaffold` 的 `containerColor` 默认是主题背景色，因此自定义底层背景需要显式使用透明容器。
  https://developer.android.com/reference/kotlin/androidx/wear/compose/material3/AppScaffold
- Android Developers：Wear OS 的 CPU/GPU 预算比手机更紧，应优先控制 Compose 动画和动态视觉效果成本。
  https://developer.android.com/training/wearables/compose/performance
- Android Developers：Wear Compose Material3 是当前推荐的 Wear Material 体系；项目使用的 1.6.x 与当前方向一致。
  https://developer.android.com/jetpack/androidx/releases/wear-compose-m3
- Kyant0/AndroidLiquidGlass：项目当前已经使用的 Backdrop 2.0.1 是 Compose Multiplatform Liquid Glass 库，Apache-2.0；因此磨砂模式直接复用现有 Backdrop，而不是再叠加第二个第三方模糊框架。
  https://github.com/Kyant0/AndroidLiquidGlass

## 仍建议在真实设备验证

1. Galaxy Watch / Pixel Watch 等真机上用 Macrobenchmark/JankStats 或开发者 GPU profiling 对“轻量/均衡/增强”三档做帧时间对比。
2. 验证 Android 13+ 上液态/磨砂开启与关闭、主题背景/图片背景四种组合。
3. 验证所有课表的全局调休与批量操作，并重点覆盖 DATE_ONLY、范围 override、恢复操作。
4. 运行 CI 的 `:shared:test`、`:wear:compileDebugKotlin`、lint 与 release R8；本环境因 Gradle 下载网络限制没有完成这一步。
