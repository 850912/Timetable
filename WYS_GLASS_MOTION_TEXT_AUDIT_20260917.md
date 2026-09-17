# Timetable 3.4.0 — 微思风格玻璃 / 页面动效 / 文案审查

## 参考实现核对
- 用户提供的“微思应用商店 3.7.2-CE” APK 中可见 `AndroidLiquidGlass`、`com.qx.wysappmarket.utils.liquidglass`、`liquid_blur_enabled`、`liquid_lens_enabled`、`liquid_aberration_enabled`、`liquid_vibrancy_enabled` 等符号。
- 因此本轮不再用自研 Shader，也不继续用 Haze 去“猜”微思的观感；Wear 玻璃后端改为 Kyant AndroidLiquidGlass / Backdrop 2.0.1，与参考 APK 的技术路线一致。

## 玻璃材质
- 全局 backdrop 只捕获 AppBackground。
- 普通胶囊统一经 `globalLiquidGlass()` 使用 Backdrop。
- 液态模式启用 vibrancy + 轻量 blur + lens；增强档可启用 chromatic aberration。
- 加强 Ambient highlight、轻量 inner/outer shadow 和白色表面高光，减少“只有磨砂、没有玻璃边缘”的感觉。
- Android 13 以下保留轻量渐变半透明 fallback。
- Wear 参数保持克制，避免把手机端强折射直接搬到手表造成掉帧。

## 动画
- 修正 SwipeDismissableNavHost 的 scrim：此前全透明会让前后页面文字在切换/返回手势中直接叠在一起，看起来像动画坏掉。
- 背景 scrim 18%，内容 scrim 10%，仍可看见自定义/流体背景，但页面层级重新可辨。
- 胶囊按压由短 tween 改为 graphicsLayer spring；仅缩放/透明度，不触发布局重排。

## 文字
- 设置、玻璃高级设置、批量日程、课程调节、调休、导入/导出中的冗长说明已压缩。
- 通用 Capsule subtitle 默认最多 2 行；按钮 secondaryLabel 最多 2 行。
- 重点原则：标题保留动作/对象，副标题只保留当前值或必要提示，不再重复解释页面功能。

## 构建说明
本地环境无法访问 services.gradle.org，因此不能在当前沙箱完成 Gradle 编译。需要由 GitHub Actions 做最终编译验证。
