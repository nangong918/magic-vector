**vector**
====


[Android.md](../../demo/note/Android.md)


## 基本框架

[todo.md](../todo.md)

设计模式选用：MVC？MVP，MVVM？MVI？
目前选用的框架是MVVM

- ✅ 清晰的分层（View层、ViewModel层、Model层
- ✅ 使用了Jetpack组件
- ✅ 已有Compose基础
- ❌ 混合架构（传统XML + Compose）
- ❌ LiveData状态管理分散
- ❌ 缺少统一的事件处理


MVI vs MVVM对比

| 特性         | MVVM                  | MVI                      |
| ------------ | --------------------- | ------------------------ |
| 数据流       | 双向                  | 单向（单向数据流）|
| 状态管理     | 分散多个LiveData      | 单一State                |
| 可预测性     | 中等                  | 高                       |
| 测试性       | 良好                  | 优秀                     |
| Compose适配  | 需要手动适配          | 天生适配                 |

改为MVI

- 单一数据源 （Single Source of Truth）
- 单向数据流 （Unidirectional Data Flow）
- 不可变状态 （Immutable State）
- 意图驱动 （Intent-Driven）

### 🛠️ 具体改造步骤
以 ChatActivity 为例：

#### 1. 定义MVI核心组件
首先创建MVI的基础架构：

```kotlin
// ChatIntent.kt - 定义用户意图
sealed class ChatIntent {
    data class SendMessage(val text: String) : ChatIntent()
    data class SelectImage(val uri: Uri) : ChatIntent()
    object StartRecording : ChatIntent()
    object StopRecording : ChatIntent()
    object StartCall : ChatIntent()
    object EndCall : ChatIntent()
    data class LoadHistory(val agentId: String) : ChatIntent()
}

// ChatState.kt - 定义UI状态
data class ChatState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val agentName: String = "",
    val agentAvatar: String = "",
    val chatState: RealtimeChatState = RealtimeChatState.NotInitialized,
    val isRecording: Boolean = false,
    val isInCall: Boolean = false,
    val errorMessage: String? = null
)

// ChatEffect.kt - 定义一次性副作用
sealed class ChatEffect {
    data class ShowToast(val message: String) : ChatEffect()
    data class NavigateTo(val destination: String) : ChatEffect()
    object FinishActivity : ChatEffect()
}
```

#### 2. 创建MVI ViewModel


````kotlin
class ChatMviViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    // 状态流
    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    // 副作用流
    private val _effect = Channel<ChatEffect>()
    val effect: Flow<ChatEffect> = _effect.receiveAsFlow()

    // 处理意图
    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.SendMessage -> handleSendMessage(intent.text)
            is ChatIntent.SelectImage -> handleSelectImage(intent.uri)
            is ChatIntent.StartRecording -> handleStartRecording()
            is ChatIntent.StopRecording -> handleStopRecording()
            is ChatIntent.StartCall -> handleStartCall()
            is ChatIntent.EndCall -> handleEndCall()
            is ChatIntent.LoadHistory -> handleLoadHistory(intent.agentId)
        }
    }

    private fun handleSendMessage(text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.sendMessage(text)
                _uiState.update { it.copy(isLoading = false, inputText = "") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
                _effect.send(ChatEffect.ShowToast("发送失败"))
            }
        }
    }

    // 其他处理函数...
}
````


#### 3. 创建Compose UI


````kotlin
@Composable
fun ChatScreen(
    viewModel: ChatMviViewModel = viewModel(),
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is ChatEffect.NavigateTo -> onNavigate(effect.destination)
                is ChatEffect.FinishActivity -> (context as? Activity)?.finish()
            }
        }
    }
    
    Scaffold(
        topBar = {
            ChatTopBar(
                agentName = uiState.agentName,
                agentAvatar = uiState.agentAvatar,
                onBackClick = { viewModel.processIntent(ChatIntent.Back) }
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = uiState.inputText,
                onTextChange = { text ->
                    viewModel.processIntent(ChatIntent.UpdateInput(text))
                },
                onSendClick = { 
                    viewModel.processIntent(ChatIntent.SendMessage(uiState.inputText))
                },
                isRecording = uiState.isRecording,
                onRecordClick = { 
                    if (uiState.isRecording) {
                        viewModel.processIntent(ChatIntent.StopRecording)
                    } else {
                        viewModel.processIntent(ChatIntent.StartRecording)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(padding))
        } else {
            MessageList(
                messages = uiState.messages,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
````


#### 4. 创建新的Compose Activity

```kotlin
class ChatMviActivity : ComponentActivity() {
    
    private val viewModel: ChatMviViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MagicVectorTheme {
                ChatScreen(
                    viewModel = viewModel,
                    onNavigate = { destination ->
                    // 处理导航
                }
            }
        }
    }
}
```





### 主页面

AgentList页面

Media页面

Setting页面

### AgentList

AgentChat详情

创建、设置Agent

#### Chat

文本Chat

语音Chat

视频Chat
* 记录视频流在本地缓存

### Media

本地视频记录，上传记录

远端视频live直播监控

远端m3u8缓存视频，播放m3u8视频

视频下载













































