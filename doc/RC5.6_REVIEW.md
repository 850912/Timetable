# RC5.6 review

本次修改：
- 删除旧的全量 timetable transport 接口，避免增量同步和全量同步并存
- 增加 SyncTransportProvider，统一创建同步通道
- 保留 WearOsTransport 作为当前唯一实际 Transport

检查：
- SyncTransport 引用检查
- SyncManager API 检查
