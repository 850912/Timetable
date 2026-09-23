# Timetable 3.5.3 Wear Fixed — Final Build Report

日期：2026-09-23

## Release candidate

名称：`Timetable-3.5.3-wear-fixed-Final.zip`

版本：3.5.3
Build code：3050300

## 完成范围

- Build11 协议 Envelope / requestId 基础边界保留并继续使用。
- Build12 PHONE_APP / DOWNLOAD / BOTH 导出闭环完成。
- Build13 Phone-side Profile Router + App Scheme / fallback 完成。
- Build14/15 诊断数据层与 capability 模型完成。
- Build16 诊断 UI、复制、日志导出、清理完成；Wear 开发者工具加入同步诊断。
- Build17 Google / China BLE runtime Transport 选择完成。
- Build18 ACK / timeout / bounded retry / dedupe 完成。
- Build19 发布前 Manifest、BLE lifecycle、权限、R8 配置与源代码结构审查完成。
- Build20 Final 源码包、审查报告、开源参考、真机矩阵完成。

## Transport architecture

### Google environment

`TransportSelector` → `WearOsTransport` → Google Wear Data Layer → phone/watch listener → Room `SyncApplier` → ACK

### China-compatible environment

`TransportSelector` → `ChinaCompatibleWearTransport` → BLE GATT → `ChinaWearBleService` / `ChinaWearBleReceiverService` → Room `SyncApplier` → ACK

### Protocol

业务 Envelope：

- `requestId`
- `type`
- `timestamp`
- `version`
- `payload`

BLE framing：

- version
- kind
- transferId
- sequence
- total
- payload bytes

## Reliable sync rules

- 每次业务操作只生成一个 requestId。
- retry 不生成新 requestId。
- receiver 在 Room transaction 内执行 requestId 去重。
- ACK 必须覆盖全部请求记录。
- retry 最大 5 次。
- timeout 后允许重发相同 requestId。

## Export

- ICS
- CSV
- JSON Backup
- PHONE_APP
- DOWNLOAD
- BOTH

BOTH 模式只有两条路径都成功才报告成功。

## Profile bridge

手机负责打开 App / Browser；Wear 不直接依赖目标 App 的存在。

目标：

- Douyin App URI candidate + web fallback
- Coolapk App URI candidate + user web fallback
- 其他目标 fallback 到网页

## 真机测试矩阵

| 场景 | 预期 | 当前环境状态 |
|---|---|---|
| Google Wear 同步 | Data Layer ACK 完成 | 需 GitHub Actions/真机 |
| China Wear 同步 | BLE ACK 完成 | 需真机 |
| 重复同步 | 不产生重复课表 | 代码闭环，需真机 |
| 无公网 | 设备间仍可同步 | 代码设计支持，需真机 |
| 蓝牙断开 | timeout/retry/failure | 代码闭环，需真机 |
| 手机不在线 | timeout/recovery | 需真机 |
| 手表重启 | service/bootstrap 恢复 | 需真机 |
| Phone Export | 文件到手机并导入 | 代码闭环，需真机 |
| Download | SAF 保存成功 | 代码闭环，需真机 |
| BOTH | 两路径均成功才 Success | 代码闭环，需真机 |
| Profile | App scheme/fallback | 需真机确认具体 scheme |
| ACK | 完整确认 | 协议 smoke + 代码审查 |
| Timeout | 进入 bounded retry | 代码审查 |
| Retry | <= 5 次 | 代码审查 |
| Diagnostic | 事件完整 | 代码审查 |
| Clear Logs | 清理成功 | 代码审查 |
| App restart | 日志/状态可恢复 | 代码审查 + 需真机 |

## Build verification

本次执行环境无法下载 Gradle 9.4.1，因此没有把离线失败冒充为“编译通过”。

仓库的 `.github/workflows/release.yml` 已配置：

- JDK 17
- Shared/JVM tests
- Mobile/Wear release build
- GitHub Actions Secrets 注入 release keystore
- 上传 mobile 与 Wear APK artifacts

最终发布前应以 GitHub Actions 实际 build + 真机矩阵结果为准。

## Security / compatibility limitations

1. China BLE 通道是通用 BLE GATT 兼容方案，不是厂商私有 SDK。
2. 当前没有额外应用层加密/设备认证层。
3. Google 环境存在但 Data Layer 暂时故障时，不会自动热切换到 BLE；这是有意避免无条件启动 BLE 服务和权限申请。
4. Coolapk / Douyin URI 属于候选 scheme，最终仍需在对应 App 版本上真机确认。

## Release gate

只有以下条件全部满足时，才建议标记为正式 release：

1. GitHub Actions release workflow 通过；
2. mobile / wear / shared tests 通过；
3. Google Wear 真机通过；
4. 至少一个无 Google Play services 的目标环境通过；
5. 重复请求不重复应用；
6. timeout/retry/重启恢复通过；
7. BOTH / Profile / Diagnostic 全部通过。
