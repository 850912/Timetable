# FINAL SOURCE AUDIT — 2026-09-23

## 审查范围

基于用户提供的 `最新.zip` 源码，目标是完成 Build16–Build20，并执行：

1. 首轮源码审查；
2. 修复发现的问题；
3. 二次源码审查；
4. 修复二次审查中新发现的跨模块问题；
5. 再做协议/结构 smoke audit 后打包。

## 首轮发现并修复

### A. BLE 二进制数据经过 UTF-8 转换

问题：早期 BLE 路径曾把 ByteArray 转成字符串再转回 ByteArray；任意非 UTF-8 字节都有数据损坏风险。

修复：mobile/wear BLE client 与 server 现在全程以 `ByteArray` 传输；只有应用层 JSON Envelope 在进入 framing 前才进行 UTF-8 编码。

### B. Envelope / PacketCodec 模块边界错误

问题：`ChinaWearEnvelope` / `ChinaWearPacketCodec` 位于 mobile，却被 Wear 模块引用。

修复：二者下沉到 `shared/importexport`，mobile 与 wear 共用同一协议实现。

### C. 诊断事件缺少 `@Serializable`

问题：诊断事件被 `SyncDiagnosticStore` 以 kotlinx.serialization 持久化，但 model 未标注 `@Serializable`。

修复：`SyncDiagnosticType` 与 `SyncDiagnosticEvent` 均增加 `@Serializable`。

### D. BLE 服务启动失败路径

问题：BLE Service 前台启动代码使用 lambda 内 return，结构不够明确且增加 Android lifecycle 分支审查风险。

修复：改为显式 `foregroundStarted` 布尔分支，失败后记录诊断并 `stopSelf()`。

### E. 完整 snapshot 只有单次传输

问题：增量有 retry，但“一键同步全部课表”路径没有同等级 ACK/timeout/retry。

修复：Google 与 China BLE snapshot 都统一为单一 requestId + ACK timeout + 最大 5 次 retry。

### F. AutoSync 仍固定使用 Google Transport

问题：`AutoSyncJobService` 与旧兼容管理器直接实例化 `WearOsTransport`。

修复：改为 `SyncTransportProvider.create()`，进入 Build17 环境选择逻辑。

## 二次审查发现并修复

### G. Wear 端缺少 `SyncDiagnosticLogger`

问题：Wear `ChinaWearBleService` 使用 `SyncDiagnosticLogger.record(...)`，原 logger 只存在 mobile 模块，会造成模块编译错误。

修复：增加 Wear 专用 `sync/SyncDiagnosticLogger.kt`，记录最近 20 条事件，并提供诊断文本、复制、清除。

### H. `WearProfileRequest` 位于 Wear 却被 Mobile 使用

问题：mobile Profile router / Data Layer service 引用了 `com.hufeng943.timetable.sync.WearProfileRequest`，但源码文件原本只在 Wear 模块。

修复：`WearProfileRequest` 下沉到 `shared/importexport`，mobile router 与 Data Layer service 改用 shared model。

### I. BLE 分片总数检查不统一

问题：部分服务端路径仍使用 65535 上限，而协议本身定义 8192 上限。

修复：统一使用 `ChinaWearBleProtocol.MAX_FRAME_COUNT`；响应/组包还额外限制 2 MiB。

### J. Google export / sync ACK 失败缺少诊断事件

问题：Google `PhoneWearDataLayerService` 对部分 failure 只返回 false，不记录统一诊断事件。

修复：对 SYNC ACK 与 Wear export failure 增加 `recordError` / `recordExport`。

## 二次审查检查结果

- China BLE smoke test：通过。
  - 二进制 payload round-trip（含 0x00 / 0x80 / 0xFF）：PASS
  - 超出 MAX_FRAME_COUNT：PASS
  - framing header size：PASS
- stale package reference：PASS
- BLE client/server UTF-8 误转换扫描：PASS
- Envelope/Codec 只保留 shared 实现：PASS
- Wear `SyncDiagnosticLogger` 存在：PASS
- WearProfileRequest shared 化：PASS
- `gradlew` executable bit：已恢复

## 构建验证边界

尝试执行：

`./gradlew :wear:compileReleaseKotlin --offline --no-daemon --stacktrace`

当前运行环境无法访问 `services.gradle.org`，Gradle Wrapper 需要下载 Gradle 9.4.1，最终以 `UnknownHostException: services.gradle.org` 终止。因此：

- 不能把本次结果表述为“Android Gradle 完整编译通过”；
- 已执行 Kotlin 级 BLE framing smoke test 与源码结构审查；
- 最终 Android 编译必须在 GitHub Actions / 有完整网络与依赖缓存的环境执行。

## 结论

源码已完成 Build16–Build20 目标的实现与二次审查，并修复二审发现的真实模块边界错误。当前状态应视为 **Final Release Candidate Source**，而不是已经完成真机验收的正式发布包。
