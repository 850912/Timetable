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
- 一键把指定课表及未完成学业待办单向同步到系统日历，并可一键清除 Timetable 写入的当前课表日历事件
- 一键生成并分享当前周课表 PNG 图片
- Android 手机桌面“今日课程”小组件

### Wear OS
- 今日课表、教学周、今日课程数 / 首末节 / 空闲时间两行摘要（避免圆屏截断）
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

项目继续基于现有依赖，不为 3.2.0 额外引入第三方运行时库。当前核心组件版本：Wear Compose **1.6.2**、Horologist **0.7.15**、Room **2.8.4**、Hilt **2.60**、KotlinX Serialization **1.11.0**、KotlinX Datetime **0.8.0**、Coroutines **1.11.0**、AboutLibraries **15.0.3**、MaterialKolor **4.1.1**。手表端“关于 → 开源许可证”读取 AboutLibraries 在构建时根据实际依赖生成的许可证清单。

开发者选项可通过在“关于应用”连续点击版本号 7 次进入，现显示应用版本、Android API / 屏幕 dp、Room schema、文件传输协议、支持格式、核心组件以及 Tile / Complication 更新策略。

## 开源协议

本项目采用 **MIT License + Commons Clause** 双协议，详细条款请查看 [LICENSE](LICENSE)。



## 3.2.0 Performance / Surface / Timetable

- Wear 冷启动移除前台主动 Tile 刷新，国行 Data Layer bootstrap 延后到首屏稳定后，减少 Compose / Room / Play services 竞争。
- 手表端保存或删除课表、课程、课时时立即请求 Tile / Complication 更新；手机同步完成后的原有刷新路径继续保留。
- Tile 继续依靠课程 Timeline 处理上课边界，周期 freshness 调整为 60 分钟兜底，不再作为主要实时更新机制。
- 当前课程卡加入轻量课程进度条，不增加持续脉冲动画或额外 graphicsLayer。
- 手机端课表卡新增“今日课表”视觉区，使用课程色、当前课强调和更清晰的信息层级；原有完整课程管理仍保留。
- Hybrid Data Layer、中国版 Legacy fallback、Room schema 与签名配置不变。

## 3.1.0 首页与信息页优化

- 版本号切换为语义化 `3.1.0`；Wear release 不再把 Git commit 数拼到可见版本号中，`versionCode` 仍独立用于构建递增。
- Wear 首页改为“状态优先”：正在上课时先显示课程名与剩余时间；课前先显示下一节倒计时；完整今日课表继续下滑查看。当前课程在课表内使用静态描边与更强底色突出；移除下一节课程持续脉冲动画。
- 课前倒计时按长度自动压缩为 `25分后上课` / `1时20分后上课` / `2时后上课`，按分钟边界刷新；当天课程全部结束后显示简短休息提示。
- About 长内容拆成多张短卡片；开发者选项标题/副标题允许完整换行，并显示 versionCode，避免省略号隐藏诊断信息。
- 开源组件说明同步到实际版本，许可证页面继续由 AboutLibraries 根据构建依赖生成。

### 3.1.0 Fix6 Wear UX

- 首页不再重复显示“正在上课”状态卡；正在进行的真实课程卡会自动定位并高亮。
- 日期标题支持“今日课程 / 明日课程 / M月D日课程”。
- 顶部时间改为透明背景的 Wear Material 3 TimeText。
- 修复从 Tile 多次启动可能堆叠 Activity 的问题。
- Tile 更新为更清晰的状态 + 主课程 + 次课程布局，并区分“今天无课”和“今日课程已结束”。
