# Timetable 3.1.0 静态审查

## 本轮变更
- 可见版本统一为 3.1.0；手机 versionCode 从 1 提升到 2；Wear release 使用纯语义化 versionName，versionCode 仍按 Git 提交数生成。
- Wear 首页改为“状态优先”：当前课程 / 下一节倒计时 / 当日结束提示位于完整课表之前。
- 下一节倒计时按长度自适应：`25分后上课`、`1时20分后上课`、`2时后上课`，避免圆屏长文本省略。
- 当前课程使用 2dp 静态描边与更强底色；下一节仅静态弱描边，不使用持续脉冲动画。
- 当天课程结束后显示 4 组简短轮换提示，完整课表继续向下滚动查看。
- About 长内容拆成多张短卡片，避免单个超高项目在圆屏边缘难以阅读。
- Developer Options 所有诊断卡片允许完整换行，并显示 versionName + versionCode。
- 核心开源组件与许可证说明更新；许可证清单仍由 AboutLibraries 根据实际构建依赖生成。

## 联网核对
- Wear OS 官方仍推荐针对不同表径使用响应式布局与 TransformingLazyColumn。
- 当前项目使用稳定 Wear Compose 1.6.2；不为本轮功能追踪 1.7 alpha。
- 课程状态刷新继续按分钟 / 课程边界驱动，不加入高频轮询。

## 静态检查
- 全量 XML 解析：0 错误。
- TimetablePager 新增 R.string 引用在默认 / 简中资源中均存在。
- 中英文格式化参数一致性：0 不匹配。
- AboutScreen 中 OneUiInfoCapsule 参数复核；修复了误传不存在参数会导致的 Kotlin 编译问题。
- Room schema v9、同步协议、日历同步与数据模型本轮不改。
- signing/timetable-release.jks SHA-256 与输入源码一致；签名配置字段未改。
- 本地 Gradle 构建因当前环境无法解析 services.gradle.org，未进入 Kotlin 编译阶段。
