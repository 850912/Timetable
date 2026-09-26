# Timetable 3.4.0 全源码逻辑复审（2026-09-16）

## 范围
- 解压并重新检查 shared / mobile / wear 全部源码。
- 202 个 Kotlin 文件，30 个 XML 资源文件；XML 均可解析。
- 重点复核：Room/同步、课程与课时 CRUD、调休/换课 Undo、批量日程工具、Liquid Glass/背景配置、Wear 导航/滚动/动画、Data Layer/提醒。

## 本轮发现并修复
### 1. 批量日程写入不是原子操作（已修复）
Wear `ScheduleToolsViewModel` 与手机端批量调整原先逐个调用 `upsertTimeSlot()`；如果中途数据库/同步记录写入失败，会留下“前半部分已修改、后半部分未修改”的课表。

修复：Repository 新增 `applyTimeSlotMutations()`，内部统一使用 `db.withTransaction`，普通批量修改一次提交；它不会写入调休专用 Undo journal，避免污染“恢复调休”语义。Wear 与 Mobile 的批量日程入口均改为先计算 mutations，再原子提交。

### 2. Wear 批量工具选择索引可能因数据实时变化失效（已修复）
课表被同步删除/课程数量变化时，旧 `tableIndex/courseIndex` 可能越界，导致页面退化为“课表不存在”或选择状态异常。

修复：监听课表数量与课程数量变化，索引失效时回到 0（全部课程/首个课表）。

## 复核通过的核心逻辑
- Room schema 仍为 v11，10→11 migration 与 adjustment history 表保持一致。
- 调休/永久占用/交换继续使用 `applyAdjustmentMutations()`，事务与 Undo journal 未被本轮普通批量事务混用。
- `restoreAdjustmentMutations()` 按 history 倒序恢复并在同一事务清理 journal。
- 快捷修改 SHIFT 仍在 ViewModel 强制 ±1..30 分钟，不能绕过 UI 传入 0 或超范围值。
- Liquid Glass 参数均有 DataStore 范围约束；背景模糊是根级单层，不是每张卡重复做全屏 blur。
- 动态取色配置链路仍从 Preference → AppConfig → Theme。
- 未发现 `GlobalScope`；两个 `while(true)` 位于挂起式时间/指针等待逻辑，不是 CPU busy loop。

## 仍保留的技术债（本轮未机械修改）
生产代码在 Data Layer Service、Reminder Receiver、PhoneWearSyncManager 中仍有 `runBlocking`。这些代码与 Service/BroadcastReceiver 生命周期和 ACK 时序有关，不能安全地全局替换成无主 `launch`；应作为独立重构处理并配合设备/CI 测试。

部分“复制课程/复制课表”仍由多个 Repository CRUD 调用组合完成；若未来要求复制操作具备严格 all-or-nothing 语义，建议新增 Repository 级 clone transaction，而不是在 Activity 层继续串行写入。

## 构建验证
尝试执行：
`./gradlew :shared:test :mobile:compileDebugKotlin :wear:compileDebugKotlin --no-daemon`

当前沙箱仍在 Gradle Wrapper 下载阶段因 `UnknownHostException: services.gradle.org` 中止，未进入 Kotlin 编译。因此本报告不宣称本地编译通过；需要 GitHub Actions/可联网 Gradle 环境做最终编译验证。

## 结论
本轮发现的实际批量修改一致性问题和 Wear 选择索引问题已修复。静态复审后未发现新的 P0/P1 业务逻辑缺陷；剩余项主要是生命周期相关 `runBlocking` 与复制作业原子性等技术债，适合单独迭代并配合 CI/真机验证。
