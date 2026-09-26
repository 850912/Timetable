# Timetable 3.4.0 全源码 UI / 操作逻辑 / 流畅度审查

日期：2026-09-17

## 审查范围

- `wear/`、`shared/`、`mobile/` 全部源码与资源。
- 当前工程共扫描 206 个 Kotlin 文件、32 个 XML；32 个 XML 均可解析，未发现 Git 冲突标记。
- 重点审查 Wear OS UI：背景层、玻璃层、设置结构、公共交互组件、课程卡片、列表稳定性与图片加载。

## 本轮已直接修复

### 1. UI 设置统一

新增 `UI 管理` 页面，并把视觉相关设置集中到这里：

- 主题风格
- 课表背景
- 液态玻璃高级设置
- 全局磨砂玻璃
- 动态配色
- 界面动效
- 顶部时间显示

主设置页现在只保留 UI 管理、导入/导出、语言、时间格式和每周首日等非视觉设置，避免同类设置散落在多个层级。

### 2. 玻璃效果统一到选项和开关

新增 `OneUiSwitchCapsule`，替换设置页原先的 `SwitchButton`。这样开关项与普通选项使用同一套胶囊、背景、玻璃、文字与触摸反馈，不再出现部分项目是玻璃、部分项目是实体 Material 表面的割裂。

`OneUiCapsuleSurface` 新增 trailing slot，可统一承载开关、状态等尾部控件。

`globalLiquidGlass()` 的表面由单一半透明色改为轻量分层渐变 + edge highlight / inner shadow，同时继续复用项目现有 Kyant0 Backdrop，避免再引入第二套实时模糊框架。

### 3. 修复磨砂玻璃未完整生效

发现 TextEdit、ColorSelection、Export 三处手工玻璃容器只判断 `isLiquidGlassEnabled`，在“只开磨砂玻璃”时仍会绘制不透明实体底色，导致磨砂效果被盖住。现已统一同时判断液态玻璃 / 磨砂玻璃。

### 4. 修复课程卡片磨砂模式误触发液态折射

`TimetablePager` 在液态玻璃或磨砂玻璃任一开启时都会提供 Backdrop；但 `CourseCard` 原实现只要拿到 Backdrop 就执行 vibrancy / lens，导致“只开磨砂玻璃”时当前课 / 下一节仍可能执行液态折射。

现已区分：

- 液态玻璃：按 SOFT / BALANCED / FLUID 档位限制 blur、lens、色散。
- 磨砂玻璃：只做轻量 blur，不做 vibrancy / lens / chromatic aberration。

同时进一步收紧课程卡片折射半径，降低圆屏列表滚动时 GPU 压力。

### 5. 增加低成本动效

新增统一 `rememberPressMotion()`：

- 使用 `graphicsLayer` 做轻微缩放 + alpha，避免按压时触发布局重排。
- 应用于公共胶囊、公共按钮、Watch 卡片。
- 新增“界面动效”开关，关闭后动画时长归零/公共按压动画停用。
- 新开关控件的滑块位移使用绘制层动画，而不是改变列表布局。

没有给液态玻璃 shader 参数或全屏背景增加持续动画，因为 Wear OS 上持续全屏 redraw 会明显增加功耗和掉帧风险。

### 6. 图片背景稳定性 / 内存优化

图片导入端本来会把新图片限制到 720px，但旧版本遗留路径可能仍指向超大原图。`AppBackground` 原先直接 `BitmapFactory.decodeFile()`，存在大图瞬时内存占用风险。

现增加 `decodeWearBackground()`：

- 先读 bounds。
- 按尺寸设置 `inSampleSize`。
- 使用 `RGB_565` 作为背景位图配置。
- 背景路径/模式变化时释放旧 Bitmap。
- 图片不可读时继续回退到主题光晕，不出现黑屏。

背景层仍独立于玻璃开关，因此关闭液态玻璃 / 磨砂玻璃不会导致图片背景或主题光晕消失。

### 7. 圆屏长文本溢出修复

课程列表卡片与 Watch 卡片中的课程名、地点、教师等此前有多处 `Int.MAX_VALUE + TextOverflow.Clip`，长文本会让卡片异常增高并挤占圆屏。

现将列表型卡片限制为合理的 1~2 行 + Ellipsis；详情页仍可显示完整信息。

### 8. Lazy 列表稳定 key

为语言、主题、时间格式、首日、备份文件、星期/重复等多个静态/动态列表补充 stable key，减少列表刷新时不必要的 item 重建与状态错位风险。

## 背景审查结论

### 图片背景

当前路径正确：后台解码、导入时缩放、唯一文件名、写入临时文件后校验再 rename、失效时回退主题背景。此次又补了旧大图 downsample 与 Bitmap 生命周期处理。

### “流体背景”

当前源码的 `TimetableBackgroundMode.THEME` 实际是 **静态主题光晕**，资源文案也明确写的是 static glow，并不是持续运动的流体 shader。这个实现本身没有“玻璃关闭后不显示”的依赖问题。

为了手表续航与流畅度，本轮没有把它改成无限循环的全屏流体动画。新增动效主要放在交互反馈与状态切换上，符合 Wear 的性能预算。

## 仍建议后续处理但本轮没有强行大改的项目

1. `CourseAdjustmentScreen.kt`、`ScheduleToolsScreen.kt` 等工具页存在大量压缩成单行的 Compose 代码，可维护性较差，后续应拆成统一组件。
2. 工程仍有不少中文硬编码，与已经存在的 strings 多语言体系不统一；这属于较大范围的 i18n 清理，不建议在 UI 性能修复里一次性机械替换。
3. 多个编辑/工具子页显式 `timeText = {}`，而 AppScaffold 有全局顶部时间设置。当前行为可能是为了避免编辑器拥挤，但语义不完全一致，建议后续明确“全局显示”还是“仅主页面显示”的产品规则。
4. 真实掉帧结论仍需 release / benchmark build 在实际 Wear OS 设备上用 System Trace / Macrobenchmark 验证。静态审查只能消除明显高风险路径，不能替代真机帧时间数据。

## 开源方案取舍

本项目已有 `io.github.kyant0:backdrop:2.0.1`，本轮继续复用而没有迁移 Haze：

- Kyant0 Backdrop 已集成且已有共享 backdrop 架构，改动风险更低。
- 实时折射依赖 AGSL，因此仍只在 Android 13+ 开启。
- Haze 2.0 已加入 `HazePerformanceMode`，后续若要重构整套玻璃可评估，但现在同时保留两套实时采样库反而增加 APK 与维护复杂度。

## 验证状态

- XML 解析：通过（32/32）。
- 冲突标记：0。
- 修改文件 Kotlin parser-level 检查：未发现 `expecting` / 语法结构错误。
- Gradle 编译：当前执行环境无法解析 `services.gradle.org`，Gradle 9.4.1 wrapper 无法下载，因此未能完成真实 `:wear:compileDebugKotlin` / lint / test。

