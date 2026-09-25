# Timetable RC5.5 源码审查与修正

日期：2026-09-06

## 结论

RC5.5 数据库基础已经可用，但原版本存在会阻断后续双向同步的实现缺陷。本轮已直接修复，不把设计完成当成实现完成。

## 已发现并修复

### 1. 删除逻辑与软删除设计冲突
原 Repository 使用 `DELETE FROM`，会立即丢失实体及其 revision。

修复：Repository 删除改为 `deletedAt` 墓碑；`revision + 1`，同时写 DELETE SyncRecord。

### 2. 父级删除缺少子级墓碑
仅删除 timetable 的 SyncRecord 不足以让远端删除其 courses/time_slots。

修复：删除 timetable 时对子级课程及课时逐级生成 DELETE SyncRecord；删除 course 时对其 time slots 生成 DELETE SyncRecord。

### 3. 软删除实体仍可能进入 UI
修复 DAO 查询和 relation mapper，使已删除实体不出现在业务流中。

### 4. TimeSlot 仍残留 `!!`
`TimeSlot` 的时间和星期字段在 domain model 中允许为空，原 mapper 使用 `!!` 仍可能抛 KotlinNullPointerException。

修复：改为 `requireNotNull` + 范围/非相等校验，失败时给出明确错误，而非空指针崩溃。

## 仍未完成且明确归入 RC5.6

当前 `SyncRecordEntity.payloadJson` 已有实体级 payload，但 `SyncManager`/`WearOsTransport` 仍以完整 Backup JSON 为传输载荷，而且没有 pending record → send → ack → markSynced 闭环。

另外现有 Backup DTO 不保留数据库实体 ID、revision、deviceId、deletedAt，因此不应直接把旧 Backup 格式冒充为双向同步协议。

RC5.6 应建立独立 SyncPayload 协议，直接承载稳定 entity ID 与版本信息，并实现接收端 LWW 比较及同步确认。

## 构建

本次没有把本地网络失败伪装成源码编译通过。源码包交由 GitHub Actions/用户本地环境构建验证。
