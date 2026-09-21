# Timetable 3.5.3 全量静态检查与修复记录

日期：2026-09-21

## 结论

本次检查覆盖四个模块（`mobile`、`wear`、`shared`、`watchface-nacho`），重点修复了两个可复现问题：

1. Wear OS 打开子页面时，原页面左移后长时间叠在新页面下面，随后才消失。
2. 安装应用后，在系统表盘选择器中看不到项目自带表盘。

没有改动页面布局、卡片样式、字体、颜色配置或业务数据结构。UI 侧只恢复 Wear Material 3 默认的导航遮罩行为，并补强重复导航拦截。

## 一、动画问题

### 根因

`AppNavHost.kt` 对 Wear Compose 的两层滑动遮罩做了全局覆盖：

- 背景遮罩透明度仅为 `0.18f`
- 内容遮罩透明度仅为 `0.10f`

`SwipeDismissableNavHost` 在前进动画期间会暂时同时保留前一个目标页和当前目标页。由于项目页面和 `AppScaffold` 使用透明容器，上述半透明遮罩会把旧页面完整暴露出来，视觉上就形成“旧页面左移后卡在下面，最后突然消失”。

### 修复

修改文件：

- `wear/src/main/java/com/hufeng943/timetable/presentation/ui/AppNavHost.kt`

处理内容：

- 删除 `LocalSwipeToDismissBackgroundScrimColor` 和 `LocalSwipeToDismissContentScrimColor` 的半透明全局覆盖。
- 保留单一的应用级 `SwipeDismissableNavHost`。
- 为导航宿主明确设置 `Modifier.fillMaxSize()`。
- 继续使用 Wear Material 3 主题提供的遮罩颜色和系统动画，不自行重写转场。

### 重复跳转保护

修改文件：

- `wear/src/main/java/com/hufeng943/timetable/presentation/ui/common/NavCompositionLocal.kt`

原来的 `navigateSingle()` 仅比较 `destination.route == route`。以下情况无法正确拦截：

- 带实际参数的路由，例如路由模板与 `course_detail/123` 的比较；
- 已经位于某个嵌套图内部，却再次点击进入同一个图；
- 快速连续点击造成同一流程重复压栈。

现在会同时检查：

- 当前目标及其 `hierarchy`；
- 当前精确参数路由对应的 back stack entry；
- 当前所在的嵌套 `NavGraph`。

这项改动不改变导航结构，只减少重复入栈和重复动画。

## 二、表盘不显示

### 根因

项目已经包含一个 Watch Face Format 模块，但发布链路没有真正交付它：

- `.github/workflows/release.yml` 只构建和上传 `mobile`、`wear` APK；
- `watchface-nacho` 没有被发布工作流构建、上传；
- 表盘 release 构建被硬编码为 debug 签名；
- 用户只安装 Wear 应用 APK 时，并不会自动获得独立的 WFF 表盘 APK。

Watch Face Format 表盘本质上是独立的、仅资源 APK，必须和带应用逻辑的 Wear APK 分开构建、分开安装或分开发行。

### 修复

修改文件：

- `watchface-nacho/build.gradle.kts`
- `watchface-nacho/src/main/AndroidManifest.xml`
- `watchface-nacho/src/main/res/xml/watch_face_info.xml`
- `.github/workflows/release.yml`
- 根目录 `build.gradle.kts`
- `README.md`

处理内容：

- 表盘 release 改为使用与主应用一致的外部 release 签名参数；缺少签名时直接失败，不再回退到 debug key。
- WFF 为仅资源模块，关闭没有意义的 R8/minify。
- Manifest 明确声明仅支持手表硬件：`android.hardware.type.watch`、`required=true`。
- 保留 `android:hasCode="false"` 和 WFF v1 声明。
- 补充官方模板中的表盘分类和多实例元数据。
- CI 同时构建并上传 `watchface-nacho` release APK。
- 增加聚合任务：

```bash
./gradlew assembleWearReleaseDistribution --no-daemon
./gradlew installWearDebug
```

其中：

- `assembleWearReleaseDistribution` 构建 Wear 应用和 WFF 表盘的 release APK；
- `installWearDebug` 向连接的手表同时安装 Wear 应用和表盘。

表盘安装后应在系统表盘选择器中显示为：

- 中文：`课表 · 照片`
- 英文：`Timetable · Photo`

它不会作为普通页面出现在应用内部。

## 三、全量静态检查结果

检查范围：

- Kotlin：207 个文件
- Gradle Kotlin DSL：6 个文件
- XML：37 个文件
- JSON/Room schema：11 个文件
- GitHub Actions YAML：1 个文件

通过项目：

- 所有 XML 均可解析；
- 所有 JSON/Room schema 均可解析；
- GitHub Actions YAML 可解析；
- 213 个 Kotlin/KTS 文件的括号、方括号、花括号及注释/字符串边界检查通过；
- 四个 Gradle 模块路径均存在；
- Manifest 中的本地应用组件均能在源码中找到；
- Wear 端只有一个应用级 `SwipeDismissableNavHost`；
- Wear 端使用 `androidx.wear.compose:compose-navigation`；
- 76 个 `NavRoutes` 引用均有定义；
- WFF Manifest、metadata、图片资源和字符串引用均可解析；
- 四张表盘图片均为 450 × 450；
- 未发现 release 使用 debug 签名；
- 未发现硬编码签名密码；
- 未发现 `TODO`/`FIXME`。

保留项：

- 三处 `Thread.sleep()` 位于旧 Wear Data Layer 的显式阻塞连接/重试代码中，其中启动调用在独立工作线程执行。它们不是本次页面转场卡住的原因，因此未为此改动同步协议。

详细机器检查结果见：`static-audit.txt`。

## 四、尚未在当前环境执行的检查

当前处理环境没有 Android SDK，也没有已缓存的 Gradle 9.4.1 分发包和项目依赖，因此不能在此处完成 Android Gradle Plugin 配置、AAPT2、Lint、单元测试和真机安装构建。

已执行的补充语法检查没有发现新修改文件的 Kotlin 解析类错误；出现的编译诊断均来自当前环境缺少 AndroidX/Compose 类路径。

在本地 Android Studio 中建议执行：

```bash
./gradlew :shared:test :mobile:testDebugUnitTest :wear:testDebugUnitTest
./gradlew installWearDebug
```

随后重点验证：

1. 首页进入“调休”“课程调节”“设置”等子页面时，旧页面不再悬停叠加；
2. 快速连续点击同一入口不会重复压栈；
3. 从屏幕左缘返回时仍使用 Wear OS 原生 swipe-to-dismiss；
4. 系统表盘选择器中出现“课表 · 照片”；
5. 表盘“下一节课”复杂功能在安装 Wear 应用后能绑定默认 provider。

若设备曾安装过由 debug key 签名的同包名表盘，而现在改装 release key 版本，Android 会拒绝覆盖安装。此时需要先卸载旧的 `com.hufeng943.timetable.watchface.nacho`，再安装正式签名版本。
