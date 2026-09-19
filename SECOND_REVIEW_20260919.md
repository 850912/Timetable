# Timetable 3.5.0 二次审查

## 已处理
- 恢复 Wear OS 预测性返回：重新启用 `enableOnBackInvokedCallback`，移除根级/子级手工 `BackHandler` 抢占，让 Navigation/Wear Navigation 接管系统返回；恢复 Swipe-to-dismiss。
- 保留“单一动画所有者”：外层 Wear Navigation 负责全屏返回动效，内部 NavHost 不叠加第二套全屏 transition，避免 ghost frame/重复动画。
- 自定义图片背景新增“模糊背景”和“流体背景”；图片仍限制尺寸并在 IO 线程解码，流体层使用轻量主题环境层，避免持续动画。
- 设置页主要可见项改为资源字符串；主题名称不再由硬编码中文提供，切换语言后可同步刷新。
- 设置文案缩短；通用胶囊允许更多文本行，降低圆屏上的不必要省略。
- Liquid Glass 主要标签资源化；保留省电模式对高成本玻璃效果的自动降级。
- About/版本信息统一更新到 3.5.0；备份元数据版本同步更新。
- 设置入口与省电选项资源化，减少中英文混杂。

## 二次静态检查
- `values/strings.xml`、`values-zh-rCN/strings.xml`、`values-round/strings.xml` 均通过 XML 解析。
- 新增 `R.string` 引用均可在默认资源中解析。
- 本轮修改 Kotlin 文件的花括号结构检查通过。
- 源码主路径已无 3.4.0 残留。
- Wear manifest 已确认预测性返回开关为 true。

## 构建验证限制
尝试执行 `./gradlew :shared:test :mobile:assembleDebug :wear:assembleDebug --no-daemon`，但当前执行环境无法解析 `services.gradle.org`，Gradle Wrapper 9.4.1 无法下载，因此无法在本环境完成真实编译/设备回归。建议在可联网 CI 或本机 Android Studio 中继续执行上述三项任务，并重点实机验证 Android 16/Wear OS 的 predictive back、圆屏边缘手势、图片背景以及中英文热切换。
