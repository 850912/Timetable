# Timetable RC5.5 Implementation

## 本次实际完成

### 1. SyncRecord 数据库闭环

`shared/data/entities/SyncRecordEntity.kt`

新增/完善：

- `payloadJson`
- `(synced, updatedAt)` 索引
- `(entityType, entityId, revision)` 索引

数据库版本从 `4` 升至 `5`。

### 2. 完整 Room Migration

新增：

`shared/data/database/AppDatabaseMigrations.kt`

迁移链：

```text
1 -> 2 -> 3 -> 4 -> 5
```

其中：

- 1→2：恢复旧课表 `color`
- 2→3：保持兼容
- 3→4：加入 `updatedAt / revision / modifiedBy / deletedAt` 与 `sync_records`
- 4→5：加入 `payloadJson`

手机端和 Wear 端均注册完整 migration chain，不再使用 `fallbackToDestructiveMigration()`。

### 3. Repository 自动产生同步事件

`TimetableRepositoryImpl` 已改为：

```text
CRUD
 ↓
生成 revision
 ↓
记录 deviceId
 ↓
写入 SyncRecord
```

覆盖：

- 新建课表
- 修改课表
- 新建课程
- 修改课程
- 新建课时
- 修改课时
- 删除课表
- 删除课程
- 删除课时

每个写操作与 `SyncRecord` 写入使用同一个 Room transaction，避免出现“数据已经修改但同步事件没有写入”的半成功状态。

### 4. revision 规则

新对象：

```text
revision = 1
```

已有对象更新：

```text
revision = oldRevision + 1
```

删除事件：

```text
revision = oldRevision + 1
operation = DELETE
```

### 5. deviceId

手机和 Wear 均使用系统 `ANDROID_ID` 作为本设备同步标识，Repository 不再使用固定 `UNKNOWN` 作为写入设备。

### 6. 删除事件

本阶段仍保留原项目的硬删除行为，删除前先产生完整 tombstone payload：

```text
entityId
revision
updatedAt
modifiedBy
deletedAt
```

真正的软删除/7 天恢复仍属于 RC5.8 之前的独立阶段，不在本 RC5.5 偷混实现。

## 验证结果

### SQLite migration simulation

已对旧版 v3 数据执行等价的 3→4→5 SQL：

- 旧 `time_tables` 数据保留
- 旧 `courses` 数据保留
- 旧 `time_slots` 数据保留
- `sync_records` 创建成功
- 两个 SyncRecord 索引创建成功
- `payloadJson` 成功加入

### 静态源码检查

- Repository transaction：PASS
- SyncRecord 写入：PASS
- DELETE 事件：PASS
- Mobile migration registration：PASS
- Wear migration registration：PASS
- 业务层绕过 Repository 的直接课程数据写操作：未发现
- 数据库版本：5

### Gradle

本环境没有继续执行 Release build；构建交给 GitHub Actions/本地 Termux。之前环境缺少 Gradle 9.4.1 分发包且无法访问 `services.gradle.org`，因此不虚报编译成功。

## 下一阶段

### RC5.6

- 根据 pending SyncRecord 生成增量同步 payload
- Wear ↔ Phone 双向同步
- LWW 冲突比较
- 手机优先 / 手表优先策略
- 同步完成后的批量 `markSynced`

### RC5.7

- Samsung Accessory Framework

### RC5.8

- soft delete / 7 天恢复
- 设置、帮助、About 的“手表同步”文案统一
- Release 真机回归


## RC5.5 审查修正（2026-09-06）

本次审查发现并修复：

1. 删除操作此前仍为物理 DELETE，与 `deletedAt` 软删除设计矛盾。现改为保留实体墓碑，并递增 revision。
2. 删除课表/课程时必须为子级课程/课时生成墓碑，否则远端可能永久保留孤儿数据。
3. 查询层现在隐藏 `deletedAt != null` 的实体；关系映射同时过滤被删除的子实体。
4. `TimeSlot` 映射此前仍使用 `!!`。现改为显式参数校验，避免 Kotlin 空指针崩溃。
5. Repository 删除事件使用与实体更新一致的 `updatedAt/revision/modifiedBy/deletedAt`。

### RC5.6 明确未完成项

`SyncRecord` 当前已经记录了增量 payload，但 `SyncManager`/`WearOsTransport` 仍采用完整 JSON 快照发送，尚未读取 pending SyncRecord，也没有成功后的 `markSynced` 闭环。

因此当前版本不能称为“增量双向同步”；RC5.6 必须增加基于稳定 entity ID 的同步 payload、远端应用逻辑、LWW 比较以及 SyncRecord 确认机制。
