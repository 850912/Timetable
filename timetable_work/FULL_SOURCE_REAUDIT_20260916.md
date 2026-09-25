# Timetable 3.4.0 — 修改后全源码复审（2026-09-16）

## 本轮修改

1. 将课表背景提升到 `AppNavHost` 根层：纯色、主题流体色场、自定义图片现在覆盖所有 Wear 页面，包括设置、关于、编辑、调课等路由。
2. 统一课程页与其它页面的背景亮度/可读性遮罩公式：主题强度随 `backgroundBrightness` 变化，遮罩统一为 `(1-brightness)*0.70`，避免同一设置在不同页面表现不同。
3. 保留课程页单一 `LayerBackdrop` 作为昂贵折射卡片的采样源；没有把 `drawBackdrop` 扩散到每个列表项。Backdrop 的工作模型要求显式采样源，而大量远距离 glass 节点也存在性能/渲染风险，因此普通卡继续使用低成本透明玻璃，重点卡使用 AGSL 折射。
4. Mobile 端再次清理用户可变长文本截断：课表名、课程名、课程时间/地点行、全部课程摘要不再强制 2 行 + ellipsis。

## 全源码复审范围

- Kotlin：202 个文件（shared/mobile/wear）
- XML：30 个文件，全部可解析；未发现 strings.xml 重复 key。
- 复查：Room v11 / 10→11 migration、调课原子事务、Undo、动态取色链路、Liquid Glass API gate、长文本、动画、rotary/虚拟表圈、同步/提醒后台路径。

## 结论

- 未发现新的 P0/P1 业务逻辑问题。
- 动态取色链路仍完整：Preference → AppConfig → MainActivity → TimetableTheme → Wear Material3 `dynamicColorScheme(context)`。
- 调课/永久换课仍通过 `applyAdjustmentMutations` + Room `withTransaction`，Undo history 仍在 Room v11。
- Wear 动画未发现自定义横向导航动画回归；主要保留官方 SwipeDismissableNavHost、列表滚动、日期回弹和必要的可见性动画；无 infinite transition。
- `while(true)` 两处均位于挂起/事件等待循环，不是 CPU busy-loop。
- 没有 `GlobalScope`。

## 仍保留的技术债（非本轮回归）

- Data Layer Service、Reminder Receiver 和 `PhoneWearSyncManager` 仍有生产 `runBlocking`。需要单独按 Service/Receiver 生命周期重构，不能机械替换成无归属 `launch`。
- 真 Liquid Glass 仍只用于重点元素；这是刻意的 Wear 性能策略，而非遗漏。全局视觉统一由低成本透明玻璃承担。
- 真实 GPU 帧时间、三星虚拟表圈事件和动态取色最终效果仍需 Galaxy Watch7 真机验证。

## 构建验证

已尝试：

`./gradlew --no-daemon :shared:test :mobile:testDebugUnitTest :wear:testDebugUnitTest`

当前执行环境在下载 Gradle 9.4.1 时因 `services.gradle.org` DNS `UnknownHostException` 中止，未进入 Kotlin 编译，因此本报告不声称 CI/编译通过。建议用 GitHub Actions 做最终编译门禁。
