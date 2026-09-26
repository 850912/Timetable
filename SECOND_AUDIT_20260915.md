# Timetable 3.4.0 二次源码审查（2026-09-15）

审查基准：`Timetable-3.4.0源码问题清单_已排除签名问题(1).txt`。
本轮是在签名 Secrets/CI 修复版上继续修改，并按原清单 1–20 逐项反查。

## 结论

- 原清单 1–17：均已落实源码修复。
- 原清单 18–19：已补针对同步、幂等、冲突、tombstone、完整导入、CSV/ICS 边界场景的回归测试。
- 原清单 20：`.github/workflows/release.yml` 已补齐，并在签名 Release 构建前先执行 JVM 回归测试。
- 二次静态反查未发现原清单问题的残留引用或已知明确回归。
- 当前沙箱无法解析 `services.gradle.org`，Gradle Wrapper 9.4.1 无法下载，因此这里**不能声称完整 Gradle 编译/测试已经实际跑通**。最终编译验收由已加入仓库的 GitHub Actions 完成；测试失败时不会继续产出 Release APK。

## 逐项处理状态

1. **NavRoutes.scheduleTools 不存在** — 已修复；源码中不再调用不存在的 `NavRoutes.scheduleTools(...)`。
2. **MORE_ABOUT_DEVELOPER_PROBE 残留** — 已移除残留调用/入口；二次 grep 无命中。
3. **SyncApplier semantic match 旧数据覆盖新数据** — semantic match 仅用于身份解析，之后统一按 `revision -> updatedAt -> modifiedBy` 裁决；新增数据库级回归测试。
4. **完整课表重复导入幂等风险** — `ImportService.importReplacingMatchesAtomic` 支持稳定 `requestId`，检查、替换、登记在同一 Room transaction 内；新增重复完整导入测试。
5. **processed_sync_requests 未形成闭环** — `SyncApplier.applyOnce` 与完整快照导入均在事务内完成“检查 requestId -> 应用 -> 登记 requestId”。
6. **完整导入/增量同步实体身份不统一** — `Timetable/Course/TimeSlot/AcademicEvent` 模型及 Backup DTO 保留稳定 `syncId`；恢复/完整同步不再无条件生成新身份。
7. **SyncResult.Success 过早** — 手机 Wear transport 的 DataItem 被接受后不再直接返回 Success；等待对端数据库应用后的 `SyncAck`，超时/ACK 不完整返回 Failed；完整快照也等待导入 ACK。
8. **CSV 非法字段静默默认** — CsvImporter 改为严格解析，非法星期、时间、重复规则、日期、override JSON 均按行汇总报错并拒绝导入，不再替换成 Monday/08:00/09:40。
9. **ICS UTC/TZID 偏移** — `Z` 按 UTC，`TZID` 按来源时区解析后转换到设备时区，无 TZID/Z 的值按 RFC floating local time 处理；加入 UTC/TZID 测试。
10. **ICS folded line** — VEVENT 解析前执行 RFC 5545 unfolding；加入折叠 SUMMARY 测试。
11. **单次 ICS 误判每周** — 单次和不规则日期使用 `DATE_ONLY` + exact date override；连续周事件才推断 EVERY_WEEK，隔周连续事件才推断奇/偶周；加入单次、不规则、连续周课测试。
12. **ICS semesterStart 语义不一致** — 统一保存最早课程日期所在周的周一。
13. **调节历史 SharedPreferences 快照脆弱** — 改为带单调序号的持久化操作栈，每次修改都保存前态；恢复时从新到旧回放，同时兼容旧历史值。
14. **永久占课缺少冲突检查** — 永久 OCCUPY/SWAP 写库前检查同星期、时间重叠、周模式/指定日期重叠；冲突时拒绝操作。
15. **Room schema 4→10 缺失** — `shared/schemas/.../1.json` 至 `10.json` 已齐；新增 `MigrationTestHelper` 的 1→10、6→10 migration instrumented tests。4→10 历史 schema 为按现有 migration DDL 重建的回归 fixture；下一次成功 Gradle/KSP 构建会继续验证当前 schema。
16. **AAPT2 强制 `/usr/local/bin/aapt2`** — `gradle.properties` 中 override/fromMaven 强制项已移除，交由 AGP/SDK 正常解析。
17. **Kotlin 2.4.0 / KSP 2.3.7 风险** — KSP 更新为 2.3.12；KSP 后续版本已包含 Kotlin 2.4.0 相关修复。
18. **同步核心自动化测试不足** — 新增 semantic-match 冲突、requestId 幂等、DELETE/tombstone、防旧数据复活、SyncManager retry/compact、完整快照稳定身份/重复导入测试。
19. **导入导出边界测试不足** — 新增非法 CSV、ICS folded line、UTC、TZID、单次事件、不规则事件、连续周期课、Backup stable syncId round-trip 测试。
20. **缺少 GitHub Actions workflow** — Release workflow 已存在；使用 GitHub Secrets 临时恢复 JKS，先跑 JVM tests，再构建 mobile/wear Release APK，最后删除临时 JKS。

## 二次审查执行项

- 搜索 P0 残留路由：0 命中。
- 搜索 `aapt2Override` / `aapt2FromMaven`：0 命中。
- 搜索仓库内 `.jks/.keystore/.p12/.pfx`：0 个。
- 搜索 `storePassword/keyPassword` 明文赋值：0 命中。
- Room schema JSON 1–10：全部可解析。
- Version Catalog TOML：可解析。
- GitHub Actions YAML：可解析。
- KSP：2.3.12。
- JVM/Release workflow：已配置测试失败即阻断 Release。
- 本地 Gradle 尝试：因 `UnknownHostException: services.gradle.org` 在 wrapper 下载阶段终止，未进入项目编译阶段。

## 仍需在 GitHub Actions 得到的最终机器验证

GitHub Runner 网络可用后，workflow 会实际执行：

`./gradlew --no-daemon :shared:test :mobile:testDebugUnitTest :wear:testDebugUnitTest`

随后才执行：

`./gradlew --no-daemon :mobile:assembleRelease :wear:assembleRelease`

因此 GitHub Actions 的绿色结果才是完整编译/单元测试/签名 Release 的最终机器验收。
