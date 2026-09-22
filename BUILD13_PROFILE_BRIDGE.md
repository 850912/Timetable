
# Build13 Profile Bridge

完成内容：

- Wear个人主页入口抽象
- 手机侧Intent路由
- 保留网页fallback设计

流程：

Wear
 ↓
ProfileRequest
 ↓
Transport
 ↓
Mobile Router
 ↓
Intent/App/Web

支持：
- 酷安
- 抖音
- 项目主页

后续Build14接入诊断日志。
