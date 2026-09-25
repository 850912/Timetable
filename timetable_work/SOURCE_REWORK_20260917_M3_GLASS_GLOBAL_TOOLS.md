# 3.4.0 源码重构补充（2026-09-17）

本轮继续以实际源码为准，并联网核对 Android 官方资料与成熟开源实现。

## 本轮落地

- “批量调节课程”“调休”保持全局范围：全部课表、全部课程、全部课时；不提供单课表 scope。
- “课程调节”移除课表切换/作用课表选择。A 课从所有课表当天的实际 occurrence 中统一发现，选中 A 后仅用其真实所属课表完成原子写入，避免跨课表搬运 courseId 破坏数据关系；恢复仍覆盖全部课表。
- 设置新增“条件 UI”，默认开启。开启后玻璃高级页只显示当前状态真正生效的参数，例如液态玻璃关闭时隐藏折射/色散等无效项，增强档才显示色散项；关闭后可查看全部高级参数。
- 条件 UI 通过 AppConfig → DataStore → AppConfigViewModel → UI 全链路持久化。
- 保留并统一现有 Kyant0 Backdrop 2.0.1，不额外叠第二套实时玻璃框架。该库当前 2.0.1 与 Compose 1.12 对齐；Haze 2.0 也已提供 blur/glass 与性能模式，但在 Wear 同时引入两套 backdrop 会增加维护和 GPU 路径复杂度，因此本轮只作为对照，不混用。
- Wear 继续采用单 backdrop、低 blur、低 lens、按性能档位限制色散/折射；这与 Android 官方 Wear Compose 性能建议（Baseline Profile/R8、减少昂贵重组和动态效果）一致。
- Tiles 保持 ProtoLayout/Timeline 路径，不把 Compose UI 硬塞进 Tile；官方文档明确 Tiles 在远端环境声明式渲染，且不应频繁抓取内容。

## 联网核验

- Material 3 / M3 Expressive: https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose Material3 releases: https://developer.android.com/jetpack/androidx/releases/compose-material3
- Compose performance: https://developer.android.com/develop/ui/compose/performance
- Wear Compose performance: https://developer.android.com/training/wearables/compose/performance
- Wear Tiles: https://developer.android.com/training/wearables/tiles
- Tiles best practices: https://developer.android.com/design/ui/wear/guides/surfaces/tiles/bestpractices
- AndroidLiquidGlass / Backdrop: https://github.com/Kyant0/AndroidLiquidGlass
- Haze: https://github.com/chrisbanes/haze

## 验证限制

本环境的 Gradle wrapper 需要下载 Gradle 9.4.1，但容器无法解析 services.gradle.org，因此不能声称完整 Gradle 编译已通过。已做源码树静态检查、XML 解析、冲突标记检查。建议 CI/本机继续跑 `:shared:test :wear:compileDebugKotlin :mobile:compileDebugKotlin`、lint、release R8，以及真机 Macrobenchmark/JankStats。
