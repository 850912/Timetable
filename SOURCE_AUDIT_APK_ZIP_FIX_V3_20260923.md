# Timetable 3.5.3 APK ZIP Fix V3 Audit

修改范围：
- wear/build.gradle.kts

保留：
- Wear UI
- AppNavHost
- 背景系统
- 动态效果
- ABI split 配置
- 签名流程

修改：
- 增加 Android packaging resources 配置，减少 release 打包阶段 META-INF 资源冲突风险。

未修改：
- 业务逻辑
- Compose UI
- 导航结构
- 数据层

复查：
- workflow 已包含：
  - Gradle cache disabled
  - clean build
  - no-build-cache
  - no-parallel
  - unzip APK integrity check

下一步：
GitHub Actions 构建后检查 unzip 验证结果。
