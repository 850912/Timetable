# Wear 执行原则 Phase 0–8 审查与实施记录

日期：2026-09-24
基线：Timetable 3.5.3 Wear / google-crossfade-checked

## 总结

本轮按“执行原则”从源码基线开始，重点完成了共享 Next Course 数据层、首页下一课信息、Tile / Complication 统一、课程提醒、Quick View、日志统一与静态回归。没有引入第二套视觉系统；新增 Compose 页面继续复用现有 `OneUiCapsuleSurface`、Wear Material 3、Theme、动态色、Liquid Glass 与现有导航动画语言。

## Phase 0：源码基线

- 工程：AGP 9.2.1、Kotlin 2.4.0、Compose BOM 2026.06.01、Compose Material3 1.4.0。
- Wear：Wear Compose Material3 1.6.2、Tiles 1.6.2、ProtoLayout 1.4.2、Complication data source 1.3.0。
- 架构：Room/Repository -> ViewModel -> Compose；Tile / Complication 直接从 Repository 取数据。
- 发现的主要结构问题：Home、Tile、Main Complication、Current/Next Complication 分别维护“当前/下一节”判断，存在跨日/跨周与边界规则分叉风险。

## Phase 1：Next Course 单一数据源

新增：

- `shared/model/NextCourseEngine.kt`
- `NextCoursePhase`
- `NextCourseOccurrence`
- `NextCourseState`

能力：

- 当前课程
- 下一课程
- 剩余分钟
- 距下一课分钟
- 当前课程进度
- 当日课程数
- 当天无课后的未来课程检索
- 跨周检索
- 时区感知
- 复用 `Timetable.resolveDate()` 的单/双周、取消、加课、修改等既有规则

接入：Home ViewModel、Tile、Main Complication、Current/Next Complication、Reminder。

测试文件：`shared/src/test/kotlin/.../NextCourseEngineTest.kt`，覆盖课中、课间、无课日跨周、单双周、时区、空课表。

## Phase 2：首页体验

首页增加共享 Next Course 状态卡：

- 课中：课程名、剩余时间、地点、下一节
- 课前/课间：下一节、时间、地点、倒计时
- 今日无课：跨日显示下一节
- 今日课程结束：结束状态

UI 继续使用现有胶囊、主题与 Liquid Glass，不建立独立视觉风格。

## Phase 3：Tile

- 保留 `Material3TileService`。
- 课程解析改为 `NextCourseEngine.occurrencesForDate()`。
- 初始当前/下一节改为共享状态。
- 保留 Timeline，在已知课程时间边界切换布局，避免依赖高频主动刷新。
- 保留 30 分钟 freshness 作为容错刷新。

## Phase 4：Complication

- Main Complication 课程时间线改为共享日期解析。
- Current / Next Complication 改为共享 NextCourseState。
- Next Complication 使用动态 `TimeDifferenceComplicationText` 倒计时。
- Current Complication 保留 ranged progress。
- 数据修改后通过 `WearSurfaceRefresher` 主动请求更新。

## Phase 5：Reminder

新增 Wear 课程提醒：

- 设置：关闭 / 提前 30 / 15 / 5 分钟。
- 默认关闭，不改变旧用户行为。
- Android 13+ 在启用提醒后请求通知权限。
- 通知包括课程名、开始时间、地点、振动与“打开课表”动作。
- 使用 `AlarmManager.setAndAllowWhileIdle()`；不申请 exact-alarm 特权，降低权限与续航成本。
- 课程数据变化后重排。
- App 启动时重排。
- BOOT_COMPLETED / MY_PACKAGE_REPLACED / TIME_SET / TIMEZONE_CHANGED / DATE_CHANGED 后重排。
- 使用 14 天滚动窗口；DATE_CHANGED 会持续滚动补充后续提醒。

## Phase 6：Quick View

Home 改为 3 页：

1. 主课表
2. 全天速览 Quick View
3. More

Quick View：

- 独立跟随本地“今天”，不修改主课表已选择日期。
- 一页顺序呈现当天所有课程。
- 显示时间、地点。
- 当前课使用强调效果，下一课使用选中状态。
- 无课程时显示下一节摘要。
- 页面只读，减少小屏交互成本。

## Phase 7：Wear OS / UI

- 继续使用 Wear Material 3、动态色、现有 MotionScheme、Rotary、Swipe / predictive-back 体系。
- 没有为了新增功能引入第二套卡片或导航语言。
- Tile 已采用 Material 3 Tile service + ProtoLayout Material 3。
- 本轮不盲目升级 Wear Compose 版本：当前源码依赖 1.6.2；由于本环境无法完成 Gradle 下载和编译验证，避免在功能改造之外增加未经验证的依赖迁移变量。

## Phase 8：代码质量

- 新增 `AppLogger`，Wear 模块的导航、同步诊断、About/DeveloperOptions 统一通过一个日志入口。
- Next Course 的分钟 ticker 集中在 ViewModel，移除首页私有的重复判断函数。
- Quick View 与主课表分离状态，避免页面间互相改日期。
- Settings 新页面继续走单一 App NavHost，不增加嵌套 NavHost。

## 静态回归结果

通过：

- Wear `res` XML 解析。
- Manifest XML 解析。
- 中英文 strings 无重复 resource name。
- Reminder 权限、Receiver、设置路由均已注册。
- Quick View 路由/首页 pager 接入存在。
- Next Course Engine 已被 Home/Tile/Complication/Reminder 使用。
- time-sensitive surface 中未保留原来的私有 `currentAndUpcoming` / `calculateCourseStatus` 逻辑。
- 除 `AppLogger` 外无直接 `android.util.Log` 调用。
- 主要修改 Kotlin 文件括号/圆括号静态平衡检查通过。

## 最终 Gradle 校验

执行：

```text
./gradlew :wear:assembleDebug :shared:testDebugUnitTest --stacktrace
```

结果：未进入项目编译阶段。Gradle Wrapper 尝试下载 `gradle-9.4.1-bin.zip` 时，当前执行环境对 `services.gradle.org` 返回 `java.net.UnknownHostException`。

因此当前可以确认的是“源码静态审查通过”；不能宣称 `assembleDebug` 或 JUnit 已实际通过。完整错误保存在 `doc/final-gradle-check-2026-09-24.log`。

在可联网开发机上的最后一步应原样执行上面的命令；若 Wrapper 与依赖已缓存，也可离线构建。之后按执行原则对首页、Tile、Complication、Reminder、设置、导入导出、同步和 Wear 交互做真机回归。
