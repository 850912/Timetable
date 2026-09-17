# Timetable 3.4.0 实际源码复核（2026-09-17）

本轮以源码为准，用户提供的问题清单只作为检索入口，不按清单机械修改。扫描 206 个 Kotlin 文件，并复核 mobile/shared/wear 的同步、UI、背景与玻璃路径。

## 清单复核

- **确认：AutoSyncJobService 生命周期问题。** 原实现的 `onStopJob()` 不取消正在运行的协程，且异常路径可能跳过 `jobFinished()`。已改为按 jobId 保存并取消 Job；正常完成调用 `jobFinished(false)`，异常调用 `jobFinished(true)`，系统 stop 导致的协程取消不重复 finish。
- **确认：SyncCoordinator 互斥范围过小。** 原 Mutex 是实例字段，而 Job 每次新建 Coordinator。已改为进程级共享 Mutex，避免同进程内 Job/重连/手动触发并发修改 retry/ACK 状态。
- **部分成立：retryCount >= 5。** DAO 的 pending 确实排除 >=5，但 mobile `MainActivity` 的连接状态点击路径已经调用 `retryFailed()` 并重新 scheduleNow，因此“完全无法恢复”不准确。没有为了迎合清单而把所有失败每 15 分钟无条件清零，否则会造成永久错误无限重试。后续更合理的是按错误类型/退避策略恢复。
- **确认：SyncApplier 的事务语义与注释不一致。** 原 `applyOne=false` 不会让 Room transaction 回滚。已改为批次中任一记录无法应用时抛出异常，从而保持 applyOnce 的 all-or-nothing 语义和 processed marker 一致。
- **确认：父子依赖仍是同步协议层风险。** 排序只能保证“同一批次内”的父项先应用，不能保证 compact 后的 pending 一定携带父实体。此次没有伪造父实体；需要协议层 dependency closure 或接收端 deferred queue 才能彻底解决。
- **确认：BackgroundSelectPager 会吞 CancellationException。** 已显式重新抛出协程取消，普通图片解码/IO 失败仍返回 null。
- **确认但未盲修：tombstone 没有清理。** 直接按 30 天删除会让离线超过 30 天的另一设备重新带回已删除实体，因此没有加入危险的固定 TTL。正确方案需要设备同步水位/代际确认后再 GC。
- **确认：Liquid Glass 高级页缺少统一 disabled 状态。** 这是 UI 状态一致性问题，下一步应让主开关关闭/设备不支持时所有依赖项不可交互，同时保留当前值以便重新开启后恢复。

## 联网核验

Android JobService 官方文档明确要求：异步 job 返回 true 后，需要在完成时 `jobFinished()`；若系统调用 `onStopJob()`，应用应停止与该 job 相关的工作，且此时无需再调用 `jobFinished()`。

Wear OS 官方性能指南强调手表 CPU/GPU 资源受限，应实际测量富动效/动态效果，并优先 Baseline Profile、R8 等性能手段。Compose 官方性能指南建议稳定 Lazy key、缓存昂贵计算、减少不必要重组。现有玻璃库 Kyant0/AndroidLiquidGlass 本身支持 Backdrop、LiquidButton/Toggle/Slider 等模式，但它不提供高层组件，项目自己统一 OneUi 组件这一架构方向是合理的；在 Wear 上不应把桌面/手机示例中的高 blur/lens 参数原样照搬。

## 构建验证

尝试执行 `./gradlew :shared:test :wear:compileDebugKotlin :mobile:compileDebugKotlin --offline --no-daemon`，wrapper 仍试图下载 Gradle 9.4.1，并因 `UnknownHostException: services.gradle.org` 失败。因此本轮不能宣称 Gradle 编译/测试通过。
