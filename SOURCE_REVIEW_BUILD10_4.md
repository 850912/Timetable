# Build 10.4 源码修改与审查记录

## 本轮范围

签名配置和 `signing/timetable-release.jks` 按要求保持不变。

### 稳定性 / 性能
- 修复 `pendingCount()` 把 `retryCount >= 5` 永久失败项继续计入待同步的问题。
- 增加失败同步计数与手动重试。
- 增量同步在写入队列时合并同一实体的旧 pending 记录。
- 自动清理 30 天前已同步记录，并最多保留最近 500 条成功历史。
- 手机连接状态从 10 秒轮询改为 Wear peer callback + Room Flow；启动时仅做一次连接探测。
- Wear Compose 状态收集改为 `collectAsStateWithLifecycle()`。
- 移除 Wear `attachBaseContext()` 中阻塞 DataStore 的 `runBlocking`，语言设置同步镜像到 SharedPreferences。
- Wear ticker 在无课程/已结束时真正停止；课前低频阶段按有效时间边界唤醒。

### 新功能
- Room v8：课时支持按日期的 `CANCELLED / MODIFIED / EXTRA` 例外。
- 手机端可设置停课、调课、临时加课和恢复正常。
- Wear 首页显示教学周。
- 课程提醒：课前、开课持续通知、下课结束；重启/时间/时区改变后重建。
- 系统日历单向同步。
- 复制课程、复制整张课表，手机端已有课程可编辑信息和课时。
- Wear Tile 改为时间边界 Timeline。
- Current Course complication 增加 `RANGED_VALUE` 课程进度。
- 手机桌面 Widget、Tile、Complication、ICS、提醒统一使用 `resolveDate()`，确保单双周和日期例外口径一致。
- JSON/CSV 备份补充日期例外；新 CSV 同时保留学期起止日期。

## 时间规则

所有新增/编辑/导入持久化路径统一要求：`endTime > startTime`。当前版本明确不支持跨午夜课程；日期解析层也会忽略历史/异常的无效时间段。

## 审查结论

已完成：
- Kotlin 文件语法级扫描；未发现新增 parser error。
- Android XML 全量解析检查。
- Room v8 migration 注册路径检查（mobile + wear）。
- Wear `collectAsState()` 遗留扫描。
- 日期例外在 Room / sync payload / backup / CSV / ICS / Widget / Tile / Complication / Reminder 的传播路径检查。
- 原包与修改后签名相关三个文件 SHA-256 对比一致。

受当前执行环境网络限制，Gradle Wrapper 无法下载 `gradle-9.4.1-bin.zip`，因此这里无法完成真实 Android/KSP/Room 编译。GitHub Actions/你的 GitHub 构建仍应作为最终编译验证；本轮交付不声称已在本机完成 Gradle 编译。
