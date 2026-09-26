# Timetable 3.4.0 全源码与背景链路复审

日期：2026-09-16

## 覆盖范围

- wear / mobile / shared 全部生产源码与 XML 资源
- 203 个 Kotlin 文件
- 30 个 XML 文件
- 重点：课表自定义图片背景、主题光晕、全局 Liquid Glass 背景采样

## 已确认并修复的问题

### 1. 自定义图片重复使用固定文件名，导致换图后 Compose 不重新加载

旧实现始终写入 `files/backgrounds/timetable_background.jpg`，而 `AppBackground` 的 `produceState` 只以 `timetableBackgroundMode` 与 `timetableBackgroundImagePath` 为 key。第二次选择图片时路径不变，状态 key 也不变，背景可能继续显示旧 Bitmap。

修复：每次选择图片生成唯一文件名 `timetable_background_<timestamp>.jpg`，确认写入并验证可解码后才更新持久化路径；成功后清理旧副本。

### 2. 图片选择/解码链路不够稳健

旧实现使用 `OpenDocument` + `BitmapFactory.decodeStream` 两次打开 URI，再自行缩放。虽然有采样，但对现代图片格式、方向和设备 picker 兼容性不理想。

修复：
- 使用 AndroidX `PickVisualMedia`，不可用时由 AndroidX 自动回退到 `ACTION_OPEN_DOCUMENT`。
- 使用 Android `ImageDecoder`。
- 解码阶段直接限制最长边 720px。
- 强制 software bitmap，便于压缩并控制内存。
- 先写临时文件、验证尺寸可读、再 rename 为正式文件。
- 压缩失败也保证 Bitmap 回收。

### 3. 主题光晕使用固定像素坐标

旧 `GalaxyAiAmbientLayer` 的渐变终点/径向中心使用固定像素，放到不同尺寸的全屏/卡片容器时视觉覆盖不一致。

修复：改为基于实际 `size.width / size.height` 的 `drawWithCache` 静态渐变，主题光晕能同时适配手表全屏和小胶囊组件，且无持续动画。

### 4. 全局液态玻璃仍有三个直接绘制容器遗漏

以下页面仍直接使用半透明 `background(...)`，没有走全局 `drawBackdrop`：
- ColorSelectionScreen
- TextEditScreen
- ExportScreen 的预览卡片

修复：接入统一 `globalLiquidGlass()`，并只在真实 Backdrop 存在时把普通背景置透明。

## 背景链路复核

当前链路：

图片选择 -> ImageDecoder 限尺寸 -> 临时 JPEG -> 可解码验证 -> 唯一路径文件 -> DataStore 路径 -> AppConfig Flow -> AppBackground IO 解码 -> ContentScale.Crop -> 根级 LayerBackdrop -> 全局玻璃 drawBackdrop

图片失效/损坏时 `AppBackground` 会回退到主题光晕，不会留下纯空背景。

## 静态结构检查

- Kotlin：203 文件；轻量词法括号检查 0 异常。
- XML：30 文件；XML 解析 0 异常。
- Merge conflict 标记：0。
- Markdown fenced-code 污染：0。
- 私钥正文：未发现。
- 背景设置目录中旧 `OpenDocument()` 实现：已移除。

## 已知但本轮未强改的技术债

项目仍有少量生产 `runBlocking`，主要位于手机/Wear Data Layer receiver/service 与提醒 receiver；这与背景功能无直接关系，但后续可以独立做协程/服务生命周期重构。

## 编译验证限制

尝试执行：

`./gradlew --no-daemon :wear:compileDebugKotlin :mobile:compileDebugKotlin :shared:test`

当前本地环境在下载 Gradle 9.4.1 时因 `services.gradle.org` DNS `UnknownHostException` 失败，因此本轮不能声称本地 Kotlin 编译通过。最终编译应以 GitHub Actions 为准。
