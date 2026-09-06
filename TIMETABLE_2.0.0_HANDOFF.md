# Timetable 2.0.0 / RC5.5 Handoff

## 当前基线

版本线：`2.0.0`

架构：

```text
SyncManager
 ├─ WearOsTransport
 ├─ SamsungTransport (reserved)
 └─ FileTransport (reserved)
```

## RC5.4 已完成

- Google Wear 同步主入口迁移至 `SyncManager -> WearOsTransport`
- Wear Data Layer 接收 Service 独立
- 旧 `PhoneWearSyncManager` 保留为兼容 facade

## RC5.5 已完成

- Room database version = 5
- 完整 1→2→3→4→5 migration chain
- `SyncRecordEntity` 完整字段与索引
- Repository 所有 CRUD 自动生成 SyncRecord
- revision 自动递增
- 每台设备记录 `ANDROID_ID`
- DELETE 操作生成 tombstone payload
- Mobile/Wear 都不再使用 destructive migration

## SyncRecord 当前结构

```text
id
entityId
tentityType
operation
revision
updatedAt
deviceId
synced
payloadJson
```

## 尚未实现

- SyncRecord → 增量传输协议
- Phone/Wear 双向合并
- 冲突解决
- Samsung Accessory
- BLE
- soft delete / 7 天恢复
- Release 真机完整回归

## 构建

构建使用：

```bash
./gradlew :mobile:assembleRelease :wear:assembleRelease --no-daemon
```

或 GitHub Actions 当前工作流。

本次没有把本地无法联网下载 Gradle 的失败计为源码失败。

## RC5.5 源码审查修正

2026-09-06 审查 RC5.5 后已修正：
- Repository 删除改为软删除，保留 revision/deletedAt 墓碑。
- 删除课表/课程时对子级课程/课时同步生成 DELETE SyncRecord。
- 正常查询隐藏软删除实体，并过滤关系中的软删除子项。
- TimeSlot mapper 去除 `!!`，改为显式参数校验。

已确认的下一阶段阻塞项：
- SyncRecord 已记录增量 payload，但当前 WearOsTransport 仍传完整 Backup JSON。
- 当前 SyncManager 尚未读取 pending SyncRecord / markSynced。
- 当前 Backup DTO 不保留实体 ID/revision/deviceId，因此不能直接作为双向同步协议。
以上必须在 RC5.6 作为独立同步协议解决。

## RC5.6
- Incremental SyncRecord transport and ACK handshake implemented.
- Stable syncId and LWW conflict comparison implemented.
- Sync tombstones prevent resurrection after remote deletes.
- Data Layer items are retained on partial/failing application.
- Room database version is now 6; 5→6 migration adds syncId and tombstones.
- Before release, GitHub Actions should run the full Release build and generate/commit Room schemas 4–6.
