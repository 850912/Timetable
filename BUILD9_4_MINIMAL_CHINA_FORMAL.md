# Build 9.4 — Minimal China Wear Formal Sync

本版本只把已经在 Legacy Probe 中验证成功的 **GoogleApiClient + Wearable.*Api** 连接路径接入正式手机端增量同步。

## 有意保持不变

- `play-services-wearable` 仍为项目原来的 `20.0.1`，没有全项目降级。
- 没有启用 Jetifier。
- 没有修改手机/手表 UI、数据库、导入导出格式、签名密钥。
- Legacy Probe 继续独立使用 `10.2.0`，用于实际设备兼容性验证。

## 正式同步改动

`mobile/WearOsTransport.kt`：

- 改用 `GoogleApiClient + Wearable.NodeApi + Wearable.DataApi`。
- 连接最多 3 次，每次 8 秒，失败间隔 2 秒。
- 成功后再次检查 `hasConnectedApi(Wearable.API)`。
- 优先选择 `nearby` 节点。
- 保留原有 `SyncEnvelope` / `WearFileTransferProtocol` 数据格式。
- DataItem 使用唯一 requestId path，并设置 urgent。

`wear/WearOsSyncReceiverService.kt`：

- 修复一个实际路径匹配问题：发送端使用 `/timetable/file-transfer/v1/<requestId>`，接收端原先却只接受精确的 `/timetable/file-transfer/v1`。
- 现在按 `PATH_PREFIX + "/"` 匹配，确保正式增量同步 DataItem 能进入处理逻辑。

## 为什么没有把正式工程切到 10.2.0

当前项目还有多个生产代码路径直接使用现代 `NodeClient` / `DataClient` / `MessageClient`。如果只把版本号全局切成 10.2.0，会造成 API/依赖树的大范围连锁修改，并且此前已经出现 AndroidX 与旧 Support Library 的 duplicate class 问题。

本 Build 先采用最小变更验证：保留正式工程的 20.0.1 依赖，只把实际同步发送路径切到官方中国 Wear OS 页面推荐的 GoogleApiClient 相关 Wearable API。

官方中国 Wear OS 文档：
https://developer.android.com/training/wearables/creating-app-china
