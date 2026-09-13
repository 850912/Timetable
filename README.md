# Timetable - 手机 + Wear OS 课程表

Timetable 是一个面向 Android 手机与 Wear OS 手表的本地优先课程表应用。课程数据保存在 Room 中，可在手机/手表编辑，并通过 Wear Data Layer 在两端同步。

## 当前能力

### 课程、课表与学业待办
- Room 持久化课表、课程、课时和学业待办
- 每周 / 单周 / 双周重复
- 手机端手动创建、快速文本创建、编辑、删除、复制课程、复制整张课表
- Wear OS 端课程与课时编辑
- 显示教学周（第 N 周）与今日课程统计
- 日期例外：停课、调课/修改、临时加课、恢复正常
- 手机端长按课程可快速处理“今天停课 / 今日调课 / 恢复今天”
- 作业 / 考试 / 实验 / 其他待办，可关联课程、地点、备注、完成状态和独立提醒
- 课程、教室、教师输入提供本地历史建议
- 统一禁止跨午夜课时，避免不同入口产生不一致数据

### 提醒与系统集成
- 手机端课程提醒，可选提前 5 / 10 / 15 / 30 分钟
- 学业待办支持准时、提前 30 分钟、1 小时、1 天提醒；未设置具体时间时按当天 09:00 计算
- 上课开始后持续通知，下课后自动结束
- Android 12+ 在用户允许时使用精确闹钟，否则自动回退为近似提醒
- 重启、系统时间、时区和精确闹钟权限变化后自动重建提醒
- 一键把指定课表及未完成学业待办单向同步到系统日历
- 一键生成并分享当前周课表 PNG 图片
- Android 手机桌面“今日课程”小组件

### Wear OS
- 今日课表、教学周、今日课程数 / 首末节 / 大屏空闲时间摘要
- 当天学业待办会跟随日期显示；标题与副标题按小屏限制行数并省略过长内容
- Tile 使用课程时间边界 Timeline，减少固定频率无意义刷新
- Current Course / Next Course Complication
- Current Course 支持 `RANGED_VALUE` 课程进度
- Next Course 使用系统时间差文本显示动态倒计时，无需应用每分钟主动刷新
- 无后续课程时停止高频 ticker；距离课程较远时按下一个有效边界唤醒
- Compose Flow 使用生命周期感知收集，减少后台无效工作

### 导入、导出与同步
- JSON 备份/恢复（schema v3，包含日期例外与学业待办）
- CSV 导入/导出（兼容旧 CSV；新格式保留学期日期和日期例外）
- ICS 导入/导出；课程导出时会展开实际日期并应用停课/调课等例外
- 手机 ↔ 手表完整快照与增量同步
- 学业待办进入同一套增量同步和冲突处理链路
- 同一实体的未同步变更会在入队时合并
- 最多自动重试 5 次，失败项可在手机端点击同步状态重新加入队列
- 已成功同步的历史记录自动清理，避免数据库长期膨胀

## 数据库

当前 Room schema：**9**。

升级链保持非破坏迁移：

`1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9`

- v8：`time_slots` 增加 `overridesJson`，用于保存按日期的课程例外。
- v9：新增 `academic_events`，用于保存作业、考试、实验和其他学业待办。

## 构建

项目使用 Gradle Wrapper。常用检查：

```bash
./gradlew :shared:test
./gradlew :mobile:assembleRelease --no-daemon
./gradlew :wear:assembleRelease --no-daemon
```

Release 构建继续使用项目现有签名配置。

## 主要模块

- `shared/`：Room、领域模型、导入导出、同步协议、日期例外和学业待办
- `mobile/`：手机端管理、提醒、系统日历、周课表图片分享、桌面 Widget、Wear Data Layer
- `wear/`：Wear OS UI、Tile、Complication、手表端同步

## 主要开源组件

项目继续基于现有依赖，不为 Build 10.5 额外引入第三方运行时库。核心组件包括 AndroidX / Wear Compose、Horologist、Room、Hilt、KotlinX Serialization、KotlinX Datetime、AboutLibraries 和 MaterialKolor。手表端“关于 → 开源许可证”会读取构建时生成的依赖许可证清单。

## 开源协议

本项目采用 **MIT License + Commons Clause** 双协议，详细条款请查看 [LICENSE](LICENSE)。
