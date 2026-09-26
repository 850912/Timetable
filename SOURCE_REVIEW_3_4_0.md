# Timetable 3.4.0 源码审查与修改记录

日期：2026-09-15

## 已处理

1. **Galaxy Watch 课程卡**：重新设计 `CourseCard`，使用圆屏安全区、右侧静态“封面”区域、课程序号圆标与底部信息胶囊；避免旧卡片边缘被圆屏裁切，并避免持续动画。
2. **多日期课时整合**：删除独立的“多日期创建课时”入口，将多选日期合并进“编辑课时 → 日期”。多选创建后每个日期写入独立 `TimeSlot`，通过 `batchGroupId` 关联；编辑某一节时间时会询问是否同步同批其他日期。旧版合并式 DATE_ONLY 数据会在进入课时列表时拆分。
3. **快捷修改入口**：批量日程工具从课程列表移到课表列表底部的“快捷修改”。
4. **临时放假**：替换“今天全部停课”，进入后用 `Picker` 按天数滑动选择 1–30 天，确认后回到课表列表。
5. **统一提前 / 延时**：合并旧分钟设置入口，使用 -120～+120 分钟（5 分钟步进）的滑动选择；编辑课时页增加临时调时摘要，展示有效时间与生效区间。
6. **生效时间 / 日期**：本轮没有改变其数据模型和语义，只保留现有开始日期、可选结束日期与时间窗。按用户要求，在进一步改动前先征求确认。
7. **调休 / 课程调节**：在“更多”新增入口。调休可把目标日期替换为同周另一星期的课程；课程调节支持 A 课改上 B 课、换课 / 占课、仅当天 / 永久。永久模式直接更新课时归属，使主页、编辑页与同步数据一致。
8. **动态取色**：系统动态主题继续使用 Wear Material 3 `dynamicColorScheme`，并让自定义 AppTheme / Galaxy AI 背景层跟随 dynamic primary / secondary，不再被固定紫青色覆盖。
9. **精简探针 App**：删除 `legacyprobe-mobile`、`legacyprobe-wear`、主应用 probe 代码、manifest probe service/activity、诊断入口及对应构建步骤；正式 Data Layer / 文件传输保留。
10. **流畅度 / 省电**：保留并强化低频 ticker / 边界唤醒策略；课程卡使用静态绘制；主要列表补齐 rotary snap，避免额外手势层与持续动画。
11. **版本 / About / 开发者页**：版本改为 3.4.0（versionCode 3040000），Room schema 10；About 与开发者页精简为版本、运行环境、数据库和核心协议信息。
12. **Samsung 虚拟表圈**：主要列表页统一 `TransformingLazyColumnDefaults.snapFlingBehavior` + `RotaryScrollableDefaults.snapBehavior`；数字/天数选择使用 Wear Material 3 `Picker`。

## 数据兼容

Room 从 v9 升到 v10，只新增 nullable `batchGroupId TEXT`，已在手机端与手表端数据库构建器中注册 `MIGRATION_9_10`。同步 payload / SyncApplier / entity mapper 同步支持该字段。

## 签名安全

原源码包含 release keystore 与硬编码签名凭据。3.4.0 源码包已移除 keystore，并改为从 Gradle property / 环境变量读取。GitHub Actions 支持通过 Secrets 注入。生产升级包必须继续使用你原来的正式签名；详见 `SIGNING.md`。

## 本地检查状态

- 已完成源码级引用清理、旧 probe 引用扫描、Kotlin 语法解析式检查、XML 解析检查、版本 / migration / route 静态检查。
- 当前容器无法访问 `services.gradle.org`，Gradle Wrapper 9.4.1 无法下载，因此没有在本地完成完整 Android 编译。该失败发生在 Gradle 下载阶段，不是 Kotlin/Android 编译错误。
- 建议以 GitHub Actions 的 `:shared:test`、`:mobile:assembleRelease`、`:wear:assembleRelease` 作为最终编译验证。
