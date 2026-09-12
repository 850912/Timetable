# Timetable - 手机 + Wear OS 课程表

Timetable 是一个面向 Android 手机与 Wear OS 手表的本地优先课程表应用。课程数据保存在 Room 中，可在手机/手表编辑，并通过 Wear Data Layer 在两端同步。

## 当前能力

### 课程与课表
- Room 持久化课表、课程、课时
- 每周 / 单周 / 双周重复
- 手机端手动创建、快速文本创建、编辑、删除、复制课程、复制整张课表
- Wear OS 端课程与课时编辑
- 显示教学周（第 N 周）
- 日期例外：停课、调课/修改、临时加课、恢复正常
- 统一禁止跨午夜课时，避免不同入口产生不一致数据

### 提醒与系统集成
- 手机端课程提醒，可选提前 5 / 10 / 15 / 30 分钟
- 上课开始后持续通知，下课后自动结束
- Android 12+ 在用户允许时使用精确闹钟，否则自动回退为近似提醒
- 重启、系统时间和时区变化后自动重建提醒
- 一键把指定课表单向同步到系统日历
- Android 手机桌面“今日课程”小组件

### Wear OS
- 今日课表与当前/下一节课程状态
- Tile 使用课程时间边界 Timeline，减少固定频率无意义刷新
- Current Course / Next Course Complication
- Current Course 支持 `RANGED_VALUE` 课程进度
- 无后续课程时停止高频 ticker；距离课程较远时按下一个有效边界唤醒
- Compose Flow 使用生命周期感知收集，减少后台无效工作

### 导入、导出与同步
- JSON 备份/恢复（schema v2，包含日期例外）
- CSV 导入/导出（兼容旧 CSV；新格式保留学期日期和日期例外）
- ICS 导入/导出；导出时会展开实际日期并应用停课/调课等例外
- 手机 ↔ 手表完整快照与增量同步
- 同一实体的未同步变更会在入队时合并
- 最多自动重试 5 次，失败项可在手机端点击同步状态重新加入队列
- 已成功同步的历史记录自动清理，避免数据库长期膨胀

## 数据库

当前 Room schema：**8**。

升级链保持非破坏迁移：

`1 → 2 → 3 → 4 → 5 → 6 → 7 → 8`

v8 为 `time_slots` 增加 `overridesJson`，用于保存按日期的课程例外。

## 构建

项目使用 Gradle Wrapper。常用检查：

```bash
./gradlew :shared:testDebugUnitTest :mobile:lintDebug :wear:lintDebug
./gradlew :mobile:assembleDebug :wear:assembleDebug
```

Release 构建请使用项目现有的签名配置。

## 主要模块

- `shared/`：Room、领域模型、导入导出、同步协议与日期解析
- `mobile/`：手机端管理、提醒、系统日历、桌面 Widget、Wear Data Layer
- `wear/`：Wear OS UI、Tile、Complication、手表端同步

## 开源协议

本项目采用 **MIT License + Commons Clause** 双协议，详细条款请查看 [LICENSE](LICENSE)。
