# Build 9.3 Minimal — China Legacy Wear Probe

## 本版原则

本版从 Build 9.2 的中国区兼容性探针方案出发，**不修改正式 Timetable 的 Wear OS 通信依赖和同步实现**。

### 正式 App 保持不变

- `mobile`：继续使用 `com.google.android.gms:play-services-wearable:20.0.1`
- `wear`：继续使用 `com.google.android.gms:play-services-wearable:20.0.1`
- `mobile/src/.../sync/WearOsTransport.kt`：保持现代 `Wearable.getNodeClient()/DataClient` 实现
- 不启用全局 `android.enableJetifier`
- 不修改 `gradle/libs.versions.toml` 中的 `playServicesWearable`

### 只有两个独立探针使用旧版

- `legacyprobe-mobile`：`play-services-wearable:10.2.0`
- `legacyprobe-wear`：`play-services-wearable:10.2.0`
- 使用 `GoogleApiClient + Wearable.NodeApi/MessageApi/DataApi`
- applicationId：`com.hufeng943.timetable.legacyprobe`

这样可以先在真实中国区设备上验证 10.2.0 legacy Data Layer 是否可用，而不会把旧 Support Library 依赖带进正式 `mobile`/`wear`。

## 为什么这样做

Android 官方当前中国区 Wear OS 文档明确要求：如果使用 Data Layer API，中国区 Wear OS 应使用 `play-services-wearable:10.2.0`，并继续使用 `GoogleApiClient` 相关 API。

因此应该先把这个兼容路径隔离验证，而不是为了验证它把整个正式工程降级。

## 当前依赖结构

```text
正式 Timetable
  mobile ── 20.0.1
  wear   ── 20.0.1

独立中国区探针
  legacyprobe-mobile ── 10.2.0 + GoogleApiClient
  legacyprobe-wear   ── 10.2.0 + GoogleApiClient
```

## GitHub Actions

```bash
./gradlew :mobile:assembleRelease --no-daemon
./gradlew :wear:assembleRelease --no-daemon
./gradlew :legacyprobe-mobile:assembleRelease :legacyprobe-wear:assembleRelease --no-daemon
```

正式 App 与探针 APK 都使用现有 release keystore，便于进行签名一致性检查；探针使用独立 applicationId，不会覆盖正式 Timetable。

## 下一步

如果 legacy probe 在中国区 S25+/Galaxy Watch 上能够：

- `GoogleApiClient: CONNECTED`
- `hasConnectedApi(Wearable.API): true`
- `NodeApi.connectedNodes` 能看到对端
- Message/DataItem 双向成功

再根据实测结果决定是否把 legacy transport **局部接入正式 Timetable**。

如果 10.2.0 也无法连接，则不应该继续修改正式 App 的 GMS Data Layer 依赖，应转向其他传输方案。
