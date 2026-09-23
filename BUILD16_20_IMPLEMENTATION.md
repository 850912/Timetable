# Timetable 3.5.3 Wear 修复项目：Build16–Build20 实施记录

日期：2026-09-23
基线：用户提供的 Build11–Build20 开发计划，当前基线 Build15
目标：把 Build16–Build20 从“计划/骨架”推进到可审查、可打包的 Final Release Candidate 源码。

## 1. Build16 — 诊断中心 UI

已实现：

- 手机端“同步诊断中心”：显示 Google Data Layer、China BLE、蓝牙、权限、当前 Transport、最近事件。
- 支持“一键复制诊断”“导出 sync_log.txt”“清除日志”。
- Wear 端“开发者工具 → 同步诊断”入口已加入；支持查看最近 20 条事件、复制、清除。
- `SyncDiagnosticEvent` / `SyncDiagnosticStore` 保持最近 20 条记录，并补上 kotlinx.serialization 所需的 `@Serializable`。
- Google Data Layer 导出成功/失败、Profile 打开成功/失败、China BLE 同步/导出/失败路径接入统一诊断。

## 2. Build17 — Transport 自动选择与国行兼容

已实现：

- `TransportSelector` 按运行环境选择，而不是按品牌名硬编码。
- Google Play services/Wear Data Layer 可用时选择 `WearOsTransport`。
- Google 环境不可用时选择 `ChinaCompatibleWearTransport`。
- China Transport 使用原生 BLE GATT：手机和手表各自承担明确角色，并通过 Service UUID + Service Data Role 过滤，避免手机互连/手表互连误匹配。
- Android 12+ BLE 权限（SCAN / CONNECT / ADVERTISE）和 Android 11 及以下位置权限已补齐。
- BLE 包帧进入 `shared`，Wear/mobile 共用同一二进制 framing 和 Envelope/Codec。

## 3. Build18 — ACK / Timeout / Retry / Dedupe

已实现：

- 每一次业务同步只生成一个 UUID `requestId`。
- 增量同步所有重试复用相同 `requestId`。
- Google Data Layer retry 在重发前删除 request-scoped DataItem，确保超时后的重试可以再次产生 DataEvent。
- China BLE retry 使用相同 `requestId`，接收端 `SyncApplier.applyOnce()` 在 Room 事务中检查 `ProcessedSyncEntity`，保证重复请求不产生重复应用。
- ACK 必须确认全部 sourceRecordId 才算成功。
- ACK 超时有界，使用 `SyncRetryPolicy.MAX_RETRY = 5`，没有无限重试。
- 完整课表 snapshot 也改为同一个 `requestId` + 有界重试，不再只有单次发送。
- 双向 ACK：手机发送后接收手表待发送记录，再通过第二次 BLE 请求发送 `SYNC_APPLIED` 确认。

## 4. Build12 完成项

- Wear 导出目标支持 `PHONE_APP` / `DOWNLOAD` / `BOTH`。
- `BOTH` 只有手机发送和本地保存都成功才显示 Success。
- 手机 BLE 导出会同时传送用户选择的导出文件与 canonical JSON backup；手机端保存文件并执行原子导入，失败时回滚 pending 下载文件。

## 5. Build13 完成项

- Profile 请求拥有统一 `WearProfileRequest`，已下沉到 `shared`，避免 phone/wear 模块边界错误。
- 手机端根据目标包名验证 App Scheme 的实际 handler。
- Douyin 使用 `snssdk1128://user/homepage` 候选 URI，失败后走网页 fallback。
- Coolapk 使用候选 `coolmarket://u/22532694`，失败后走 `https://www.coolapk.com/u/22532694` fallback。
- 打开成功、fallback、失败均写诊断记录。
- Google Data Layer 和 China BLE 都支持 Profile Open 请求。

## 6. Build19 — 发布前稳定性

已完成静态审查：

- Manifest、Bluetooth 权限、前台服务类型、legacy Bluetooth 权限范围检查。
- BLE 分片数量限制 8192，单次组包限制 2 MiB；业务完整同步限制 512 KiB。
- Android 13+ `notifyCharacteristicChanged` / `writeCharacteristic` 使用新 API 路径，旧 API 有兼容分支。
- Service 的前台启动失败路径已改为显式分支，不依赖 lambda 中的非局部 return。
- Wear 启动时仅在 Google Play services 可用时启动 Google Data Layer bootstrap；无 GMS 的 Wear 进入 China BLE 兼容服务。
- Release 工作流已保留签名密钥通过 GitHub Actions Secrets 注入，源码不保存 keystore/password。

未声称完成的部分：当前环境无法完成真实设备功耗/启动速度/Compose 帧率基准测试；这些留给 GitHub Actions + 真机矩阵。

## 7. Build20 — Final Release Candidate

已生成：

- `Timetable-3.5.3-wear-fixed-Final.zip`
- `FINAL_BUILD_REPORT.md`
- `FINAL_SOURCE_AUDIT_20260923.md`
- `OPEN_SOURCE_REFERENCES.md`

发布闸门按：源码闭环、静态审查、协议 smoke test、Manifest/模块边界审查、GitHub Actions release workflow 与真机测试矩阵执行。

## 8. 已知限制

1. China Wear 通道是通用 BLE GATT 兼容层，不等同于 Xiaomi/OPPO/Samsung 私有厂商 SDK；这样可以减少厂商 SDK 依赖，但最终仍需在目标设备上验证 GATT 行为。
2. BLE 应用层协议目前没有额外的端到端身份认证层；它依赖 BLE 链路和已配对设备环境，不应被视为恶意射频环境下的安全通道。
3. “Google 可用”按 Google Play services/Data Layer 环境判定；本次未实现“Google 存在但 Data Layer 节点暂时故障时自动切换 BLE”的双通道后台热备，以避免在国际设备上无条件申请/启动额外 BLE 服务。
4. 当前无法在离线构建环境下载 Gradle 9.4.1，因此本机没有完成完整 Android Gradle 编译；最终编译闸门应由仓库内 GitHub Actions `release.yml` 执行。
