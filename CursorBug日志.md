**CursorBug日志**
====






| Bug | 问题描述 | 开发者期望 | 解决思路及 | cursor分析的问题原因 |
| --- | --- |-------| --- | --- |
| 打开页面无法创建Agent | Agent 首页仅显示“当前暂无消息”，未出现明确创建入口；`/agent/getLastAgentChatList` 返回参数错误。 | 空状态可直接创建 Agent；状态机由“是否有Agent+是否有消息”共同驱动。 | Android 端 `MessageListMviVm` 重构为双接口聚合：并行请求 `getAgentList + getLastAgentChatList`，新增 `MessageListUiMode(NO_AGENT/HAS_AGENT_NO_MESSAGE/HAS_MESSAGE)`；`MessageListScreen` 在 `NO_AGENT` 显示中心创建按钮，其它状态显示 FAB；增加 `userId` 兜底恢复（从本地用户会话恢复并回写 `MainApplication`）。 | 原实现仅依据“消息列表是否为空”判定页面状态，导致“有Agent但无消息”和“无Agent”混淆；且请求直接使用缓存 `userId`，空值时触发后端参数校验失败。 |
| Mine 页面布局错误 | Mine 页将 Setting/视频/测试内容混在同一页面，不符合三入口跳转设计。 | 头像下方三个按钮：设置、视频、测试，分别进入独立 ComposeActivity。 | 新增 `ComposeMineSettingActivity`、`ComposeMineVideoActivity` 并注册 Manifest；`MineScreen` 改为入口页，仅负责三按钮导航；补齐两个 Activity 的独立 MVI 编排（`ComposeMineSettingVm`、`ComposeMineVideoVm`）与专属 Screen（输入、播放、上传、云录播逻辑）。 | 先前实现将入口层与业务层耦合在一个 Composable，且仅做壳 Activity，未把业务逻辑迁移到目标页面。 |
| Control 页面状态闪烁与状态语义错误 | 进入 Control 页面后连接状态短暂变绿再变红，录制/离线提示易被重置。 | 状态稳定，不应被生命周期重建覆盖；`App-Spring` 状态应体现登录后常驻连接。 | `ControlScreen` 改为 `viewModel()` 生命周期托管，避免重组时反复 new VM；`ControlVm` 中禁止用 `/control/status` 覆盖 `App-Spring` 状态，仅由 `NetworkManager.isOnlineAndWsReady` 驱动；状态卡将 `Control WS` 展示改为 `RTMP拉流`。 | Composable 默认参数直接 `ControlVm()` 导致频繁重建，触发初始化与默认状态回写；且控制状态接口回包覆盖了真正的 App-Spring 长连接状态。 |
| Control 页面操控 RK 闪退 | 拖拽摇杆或点击前进/急停触发 NPE：`ControlCommandController.baseRequest`。 | 指令发送稳定，无空指针崩溃。 | 修复序列号赋值：`sequenceCounter.getAndIncrement()` 替代 `sequence++`；并增强 `ControlConsoleManager` 的 WS 重连稳定性（复用 OkHttpClient、忽略旧连接回调、同连接复用）。 | `apply` 作用域内 `sequence++` 实际操作了请求对象可空字段 `Long sequence`，初值为 null 导致自增时 NPE。 |







