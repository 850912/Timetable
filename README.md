# Timetable 3.5.4

Timetable 是一个 Android 手机 + Wear OS 的本地优先课程表应用。课程数据保存在 Room 中，并通过 Wear Data Layer 在手机与手表之间同步。

## 3.5.4 重点

- 手机与 Wear OS 的液态玻璃视觉全面增强；手表端提供柔和、平衡、流体三种光学档位，并开放模糊、折射、清透度、底色浓度和边缘色散调节。
- 手机端加入动态主题环境光、半透明渐变卡片和课程色彩高光，让手机与手表保持统一的视觉语言。
- 优化 Wear OS 首页触摸检测、旋转滚动、课程文本缓存和下一节课查找，减少重复计算并提升滚动流畅度。
- 导入前可预览课程、时间段和重复课表，支持新增导入与替换导入。
- 新增同步中心、失败记录重试与恢复、每日自动 JSON 备份，以及课程通知延后 5 分钟。
- 继续保留旧 Wear Data Layer 路径、国行设备 BLE 兼容协议、CRC32 v2 可选协议及 Xiaomi / 中国 ROM 触觉安全回退。
- 手机与手表 Release APK 继续使用同一签名证书，确保正式版本间可靠互传。

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
./gradlew :wear:assembleRelease --no-daemon
```

GitHub Actions 工作流位于 `.github/workflows/android-release.yml`，推送到 `main` / `master` 或手动触发即可构建手机端和手表端 APK。

### Release 签名

源码包不包含 keystore 或硬编码密码。生产更新包继续使用原来的 release key，并按 `SIGNING.md` 从本地环境变量或 GitHub Actions Secrets 注入。Release 缺少签名参数时会直接失败，不再回退到 debug 签名，避免误产出无法覆盖正式版本的 APK。

## 主要模块

- `shared/`：Room、领域模型、日期例外、导入导出与同步协议。
- `mobile/`：手机端管理、提醒、系统日历、Widget 与 Wear Data Layer。
- `wear/`：Wear OS Compose UI、Tile、Complication、手表端编辑和同步。

## Wear OS 交互说明

课程表与工具页采用 `TransformingLazyColumn` + snap rotary behavior。Galaxy Watch 的触摸虚拟表圈会作为 Wear OS rotary input 参与滚动；滑动选择器使用 Wear Material 3 `Picker`。

## 开源协议

MIT License + Commons Clause，详见 `LICENSE`。
