# Timetable RC5.5 二次源码审查

## 审查结论

RC5.5 首轮修正版仍存在影响实际同步可靠性的缺陷，本次已修复 4 项关键问题：

1. 新增实体写入 SyncRecord 时 payload 的 `id` 原来可能仍为 0，因为 Room `@Insert` 返回的新主键没有回填到不可变 Entity。现已使用 `storedEntity = entity.copy(id = insertedId)` 生成 payload。
2. `deleteTimetable()` / `deleteCourse()` 生成子实体 tombstone 时，查询已限制 `deletedAt IS NULL`，避免重复删除已删除子项并不断递增 revision。
3. Wear `WearableListenerService` 原来在导入失败时也删除 DataItem，可能导致待重试同步数据丢失。现在只有导入成功后才删除 DataItem。
4. `ImportService.importReplacingMatchesAtomic()` 原来硬删除匹配课表，违反软删除设计。现改为对旧课表及其子实体执行软删除后再写入新快照；身份查询同时只匹配未删除课表。

## 仍存在的架构边界

- RC5.5 的 SyncRecord 目前仍没有真正驱动增量同步；`SyncManager` 仍发送完整 Backup JSON。
- `ImportService` 仍属于“快照导入”路径，不是 RC5.6 的基于 revision/LWW 的远端变更应用器，因此不会伪装成双向同步已经完成。
- Room schema 目录当前只看到 1/2/3 版本 JSON，4/5 尚未导出。按 Android 官方文档，迁移应保留导出的 schema 并使用 MigrationTestHelper 覆盖完整迁移链。
- 本环境无法运行 Gradle Wrapper，因为无法访问 services.gradle.org；未声称编译通过。

## 官方依据

Android Room migration 文档要求保存导出的 schema，并建议测试完整 migration chain；Wear OS Data Layer 的 DataClient 支持离线持久同步，但应用仍应将业务数据保存在自己的 Room 数据库中。
