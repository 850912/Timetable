# Timetable 3.4.0 源码审查清单修复

本轮基于用户提供的 `Timetable-3.4.0-source-audit-issues.txt` 对完整源码逐项核对并修改。

## 已修改的生产代码问题

- `CourseReminderReceiver` / `ReminderRescheduleReceiver`：移除生产路径 `runBlocking` 和手工线程，改为 `goAsync()` + IO coroutine。
- `PhoneWearDataLayerService`：移除生产路径 `runBlocking`，Data Layer 回调先复制 `DataMap`/URI 后交由 service coroutine scope 处理；`onDestroy()` 取消 scope。
- `WearOsSyncReceiverService`：同样移除 `runBlocking`，同步 ACK、批次同步与导入改为 suspend 流程，回调不再同步阻塞数据库 I/O。
- `PhoneWearSyncManager`：废弃兼容入口改为 `suspend fun`，不再通过 `runBlocking` 桥接。
- `PhoneFileTransferActivity`：移除 `Executors.newSingleThreadExecutor()`，使用结构化 coroutine scope，并在 `onDestroy()` 取消。
- `AppNavHost`：移除图片背景渲染处 `backgroundBitmap!!`，使用局部非空值安全渲染。
- `ScheduleToolsScreen`：移除两处 `end!!`，使用安全 nullable 比较。
- `TimetablePager`：移除 `nextStartSec!!`。
- 手机备份规则：移除模板 TODO，保留课程表/设置可备份，同时排除设备绑定的 `wear_bridge_state.xml`。
- Wear 备份规则：补齐 API 31+ `dataExtractionRules` 和旧版 `fullBackupContent`，同样排除设备绑定 Wear Data Layer 路由状态。

## 保留项（不是发布包问题）

审查清单中 `shared/src/androidTest` 的 `runBlocking` 与 `!!` 位于 instrumentation tests，仅用于同步运行测试协程和断言测试前置条件，不进入生产 APK/AAB，因此保留。

`SOURCE_AUDIT_20260910.md` 中出现的 `TODO/FIXME` 字样是历史审查报告对搜索项的描述，不是源码 TODO。

## 复查结果

- 生产 Kotlin (`mobile/src/main/java`, `wear/src/main/java`, `shared/src/main/java`)：未检出 `runBlocking`。
- 同一生产 Kotlin 范围：未检出 `!!`。
- `src/main` Kotlin/XML：未检出 `TODO` / `FIXME`。
- XML 共 32 个，Python XML parser 全部解析通过。
- 未发现 merge conflict 标记。
- 本地 Gradle 编译未能执行：Gradle wrapper 仍需下载 `gradle-9.4.1-bin.zip`，当前运行环境无法解析 `services.gradle.org` (`UnknownHostException`)。最终编译结果应以 GitHub Actions 为准。

## 关于图片背景

上一轮图片背景修复继续保留：Photo Picker、解码阶段限尺寸、唯一文件名、临时文件原子落盘、写后可解码验证、成功后清理旧背景，以及背景文件失效时回退主题光晕。本轮没有回退这些修复。
