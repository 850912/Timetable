# Timetable 3.5.3

Timetable 是一个 Android 手机 + Wear OS 的本地优先课程表应用。课程数据保存在 Room 中，并通过 Wear Data Layer 在手机与手表之间同步。

## 3.5.3 重点

- Wear OS 首页课程卡重新设计为更适合 Galaxy Watch 圆屏的媒体卡式布局，减少边缘裁切。
- “编辑课时 → 日期”支持多日期选择；多选创建后每一天都会成为独立课时，并用 `batchGroupId` 记录同批关系。之后修改其中一节的时间时，可选择是否同步同批其他日期。
- “批量日程工具”移动到“课表列表 → 快捷修改”。
- “临时放假”使用按天数滑动选择；“统一提前 / 延时”使用分钟滑动选择。
- 编辑课时页会显示日期例外产生的临时调时，避免首页有效时间与编辑页看起来不一致。
- “更多”新增“调休”和“课程调节”；课程调节支持换课 / 占课、仅当天 / 永久。
- Wear Material 3 系统动态色贯穿自定义 One UI 组件，并保留静态主题作为回退。
- 移除 legacy/probe 测试互传 App 与主应用中的探针入口，只保留正式同步与文件传输链路。
- 主要列表统一使用 Wear Compose 1.6 的旋转 / 虚拟表圈 snap 行为。
- 关于与开发者页面精简，版本更新为 3.5.3。

## 数据库

当前 Room schema：**10**。

迁移链：

`1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10`

- v8：`time_slots.overridesJson`，保存按日期课程例外。
- v9：`academic_events`，保存作业、考试、实验和其他学业待办。
- v10：`time_slots.batchGroupId`，记录一次多日期创建得到的独立课时之间的可选同步关系。

## 构建

项目使用 Gradle Wrapper / JDK 17：

```bash
./gradlew :shared:test
./gradlew :mobile:assembleRelease --no-daemon
./gradlew assembleWearReleaseDistribution --no-daemon
```

Wear OS 应用与 Watch Face Format 表盘必须是两个独立 APK。调试真机时可用下面的聚合任务一次安装两者：

```bash
./gradlew installWearDebug
```

GitHub Actions 工作流位于 `.github/workflows/release.yml`，推送到 `master`、推送 `v*` 标签或手动触发后，会分别生成手机端、手表端和表盘 APK。

### Release 签名

源码包不包含 keystore 或硬编码密码。生产更新包继续使用原来的 release key，并按 `SIGNING.md` 从本地环境变量或 GitHub Actions Secrets 注入。Release 缺少签名参数时会直接失败，不再回退到 debug 签名，避免误产出无法覆盖正式版本的 APK。

## 主要模块

- `shared/`：Room、领域模型、日期例外、导入导出与同步协议。
- `mobile/`：手机端管理、提醒、系统日历、Widget 与 Wear Data Layer。
- `wear/`：Wear OS Compose UI、Tile、Complication、手表端编辑和同步。
- `watchface-nacho/`：独立的资源型 Watch Face Format 表盘 APK。

## Wear OS 交互说明

课程表与工具页采用 `TransformingLazyColumn` + snap rotary behavior。Galaxy Watch 的触摸虚拟表圈会作为 Wear OS rotary input 参与滚动；滑动选择器使用 Wear Material 3 `Picker`。

## 开源协议

MIT License + Commons Clause，详见 `LICENSE`。
