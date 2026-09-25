# Wear OS Navigation Fix v1

修改目标：
- 修复进入子页面时上一页面左移露出、卡顿后消失的问题。

本次修改：
1. 保留 Liquid Glass / Backdrop 效果，不改变视觉渲染。
2. 为 Wear 导航根容器增加 clipToBounds，限制 transition layer 越界绘制。
3. 为 SwipeDismissableNavHost 增加 fillMaxSize + clipToBounds。
4. 为 AppScaffold 增加 clipToBounds。

未修改：
- Liquid Glass
- 背景渲染逻辑
- 动画风格
- 页面结构

说明：
本版本为源码级修复，需要重新编译 APK 后在实体 Wear OS 设备验证。
