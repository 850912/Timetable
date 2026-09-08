# Build 9.4 — China Wear sync, minimal integration

这版修正上一版“Probe 能工作、正式 App 不工作”的集成方式。

## 核心原则

没有把正式工程的 `play-services-wearable` 降级到 10.2.0。正式工程继续使用当前 `20.0.1`。

只把**正式 App 实际的数据传输调用层**统一切换到已经在中国区 Probe 验证过的 `GoogleApiClient + Wearable.NodeApi/DataApi` 调用模型。这样手机和手表两端的正式传输路径一致。

Google 官方文档确认 `DataApi` / `NodeApi` 是旧版接口，后来由 `DataClient` / `NodeClient` 替代；这些旧接口在当前参考文档中仍存在并标记为 deprecated。

## 修改范围

新增：
- `mobile/.../sync/LegacyWearableClient.kt`
- `wear/.../sync/LegacyWearableClient.kt`

统一负责：
- GoogleApiClient 连接
- Wearable API 可用性检查
- NodeApi 节点发现
- DataApi put/delete
- DataApi Asset 读取
- 3 次连接重试，每次 8 秒，间隔 2 秒

正式通信代码改为调用这个兼容层：
- 手机增量同步
- 手机端 Data Layer 接收/ACK
- 手机文件传输
- 手表增量同步接收/ACK
- 手表文件传输

Probe 代码保持独立，不参与正式通信。

另外修复了带 requestId 的 DataItem path 接收端原先只匹配基础 PATH 的问题，使 `/timetable/file-transfer/v1/<requestId>` 能进入正式接收处理。

## 重要

没有修改：
- 数据库
- 同步协议数据结构
- UI
- 导入/导出格式
- release 签名
- `play-services-wearable` 20.0.1

官方资料：
https://developers.google.com/android/reference/com/google/android/gms/wearable/DataApi.GetFdForAssetResult
https://developers.google.com/android/guides/releases
