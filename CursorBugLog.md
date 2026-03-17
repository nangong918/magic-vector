**CursorBug日志**
====






| Bug | 问题描述 | 开发者期望 | 解决思路及 | cursor分析的问题原因 |
| --- | --- |-------| --- | --- |
| 打开页面无法创建Agent | Agent 首页仅显示“当前暂无消息”，未出现明确创建入口；`/agent/getLastAgentChatList` 返回参数错误。 | 空状态可直接创建 Agent；状态机由“是否有Agent+是否有消息”共同驱动。 | Android 端 `MessageListMviVm` 重构为双接口聚合：并行请求 `getAgentList + getLastAgentChatList`，新增 `MessageListUiMode(NO_AGENT/HAS_AGENT_NO_MESSAGE/HAS_MESSAGE)`；`MessageListScreen` 在 `NO_AGENT` 显示中心创建按钮，其它状态显示 FAB；增加 `userId` 兜底恢复（从本地用户会话恢复并回写 `MainApplication`）。 | 原实现仅依据“消息列表是否为空”判定页面状态，导致“有Agent但无消息”和“无Agent”混淆；且请求直接使用缓存 `userId`，空值时触发后端参数校验失败。 |
| Mine 页面布局错误 | Mine 页将 Setting/视频/测试内容混在同一页面，不符合三入口跳转设计。 | 头像下方三个按钮：设置、视频、测试，分别进入独立 ComposeActivity。 | 新增 `ComposeMineSettingActivity`、`ComposeMineVideoActivity` 并注册 Manifest；`MineScreen` 改为入口页，仅负责三按钮导航；补齐两个 Activity 的独立 MVI 编排（`ComposeMineSettingVm`、`ComposeMineVideoVm`）与专属 Screen（输入、播放、上传、云录播逻辑）。 | 先前实现将入口层与业务层耦合在一个 Composable，且仅做壳 Activity，未把业务逻辑迁移到目标页面。 |
| Control 页面状态闪烁与状态语义错误 | 进入 Control 页面后连接状态短暂变绿再变红，录制/离线提示易被重置。 | 状态稳定，不应被生命周期重建覆盖；`App-Spring` 状态应体现登录后常驻连接。 | `ControlScreen` 改为 `viewModel()` 生命周期托管，避免重组时反复 new VM；`ControlVm` 中禁止用 `/control/status` 覆盖 `App-Spring` 状态，仅由 `NetworkManager.isOnlineAndWsReady` 驱动；状态卡将 `Control WS` 展示改为 `RTMP拉流`。 | Composable 默认参数直接 `ControlVm()` 导致频繁重建，触发初始化与默认状态回写；且控制状态接口回包覆盖了真正的 App-Spring 长连接状态。 |
| Control 页面操控 RK 闪退 | 拖拽摇杆或点击前进/急停触发 NPE：`ControlCommandController.baseRequest`。 | 指令发送稳定，无空指针崩溃。 | 修复序列号赋值：`sequenceCounter.getAndIncrement()` 替代 `sequence++`；并增强 `ControlConsoleManager` 的 WS 重连稳定性（复用 OkHttpClient、忽略旧连接回调、同连接复用）。 | `apply` 作用域内 `sequence++` 实际操作了请求对象可空字段 `Long sequence`，初值为 null 导致自增时 NPE。 |
| 已有账号信息未跳转 + 登录体验不足 | 启动页每次重开都跳登录，且无法查看本地会话明细；登录页只能手输账号，不能从本地历史账号下拉选择并自动回填已保存密码。 | Start 页可打印本地数据库会话用于排查；支持“输入 + 下拉选择历史账号”；登录成功后才保存密码；SpringBoot token verify 日志可通过配置开关控制。 | Android：重构 `user_session` 为多账号结构（新增 `password/is_current/last_login_at`），`UserManager` 支持查询全部账号与当前账号；`StartActivity` 增加本地会话日志输出；登录页新增下拉历史账号并自动回填密码；登录成功保存密码，注册成功默认不存密码。SpringBoot：新增 `DebugConfig`（`openapi.debug.enabled` + `openapi.debug.token-verify-log-enabled`），仅在 debug 开启时输出 `/user/token/verify` 请求与校验结果日志。 | 旧实现的 `user_session` 只保留固定单行（`id=1`），无法承载历史账号与密码回填需求；同时 token verify 缺少可控调试日志，导致“本地会话/服务端校验”链路排障成本高。 |
| Agent请求参数错误与超时 | `/agent/getList` 返回 C_10001 参数错误、不全；`/agent/getLastAgentChatList` 请求超时 Canceled。 | Agent 相关接口正常返回数据，无参数错误和超时问题。 | 修复数据库实体类与数据库表类型不匹配问题：AgentDo.id 从 String 改为 Long，AgentDo.userId 从 String 改为 Long；ChatMessageDo.agentId 从 String 改为 Long，ChatMessageDo.userId 从 String 改为 Long；修复相关的 Mapper、Service 和 Converter 代码，确保类型转换正确。 | 数据库实体类（AgentDo、ChatMessageDo）中的 ID 字段类型与数据库表（agent、chat_message）的 bigint 类型不匹配，导致 MyBatis 查询失败或参数解析错误，进而返回 C_10001 错误；超时问题可能是由于类型不匹配导致的处理阻塞引起。 |
| 创建Agent之后主页面收到了后端的响应，但是主页没有更新 | 创建Agent成功后，后端返回了正确响应，但是 MessageListPage 页面没有立刻更新UI，仍显示 NO_AGENT 空状态。 | 创建Agent成功后，MessageListPage 应该立刻刷新并显示新的Agent列表。 | 架构修复：禁止在 `Fragment` 组合函数默认参数中 `MessageListMviVm()/ControlVm()/MineVm()` 直接 new；由 `MainVm` 统一持有三个 Tab 子 VM（`messageListVm/controlVm/mineVm`），`MainActivity -> MainActivityScreen -> 各TabScreen` 通过函数参数注入同一实例。这样创建 Agent 后触发的刷新事件和页面渲染读取的是同一个 VM 状态。 | 根因是 Composable 层默认创建 VM，页面组合重建/切换时会出现新实例，导致刷新写入和 UI 读取不在同一状态容器上，最终出现“日志已返回 hasAgent=true，但界面仍停在 NO_AGENT”的状态错位。 |







