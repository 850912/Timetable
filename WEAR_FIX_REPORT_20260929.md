# Wear 端修复记录（2026-09-29）

## 本次修复

1. **方屏液态玻璃课程卡出现矩形色块**
   - `CourseCard` 在液态玻璃绘制前显式按 `CourseCapsuleShape` 裁剪。
   - 保留原有圆角边框、玻璃效果和课程颜色，仅约束光学表面的实际绘制区域。

2. **日期选择器误触发**
   - 有课程列表时：必须同时满足“手势从屏幕顶部 64dp 区域开始”且“课程列表已经在顶部”，向下拖动才会打开日期选择器。
   - 空课表页面：只允许从顶部 64dp 区域开始下拉打开。
   - 日期选择器已经展开时仍可正常向上收起；横向滑动继续交给主页 Pager。

3. **换课后『全天速览』不刷新**
   - Home ViewModel 对 Room 课表流改为在 Home 生命周期内持续观察，避免从换课页返回时短暂拿到旧快照。
   - 全天速览列表 key 纳入“课程 + 课时 + 生效后的开始/结束时间 + 地点”，换课/调课后会建立新的可视项身份，避免 `TransformingLazyColumn` 复用旧项。
   - Home 只有 3 页，预加载相邻 1 页，让全天速览持续订阅数据，同时降低第一次横滑的加载抖动。

4. **Wear 端额外优化**
   - 节假日远程查询移动到 `Dispatchers.IO`，并且已有内置节日时不再发起网络请求。
   - 主课表和全天速览末尾增加 18dp 安全空间，避免底部 Page Indicator 遮挡最后一张卡片（方屏/短屏更明显）。

## 二次检查

- 对照原始压缩包做逐文件 diff，业务源码仅修改上述 7 个 Kotlin 文件；另新增本说明文件。
- 检查日期选择器调用链：课程列表与空页面走不同的顶部手势策略，横向手势仍不被抢占。
- Kotlin 解析级静态检查未发现 `expecting / unexpected tokens / unclosed` 等语法类错误（无 Android/Compose classpath 时其余 unresolved diagnostics 属预期）。
- 检查修改文件无冲突标记、无 NUL 字节。
- 尝试执行 `:wear:compileDebugKotlin`；当前沙箱无法访问 `services.gradle.org`，Gradle Wrapper 9.4.1 不能下载，因此这里无法完成完整 Gradle 编译。建议在 Android Studio/本地 Gradle 环境再跑一次 assemble + 方屏/圆屏实机回归。
