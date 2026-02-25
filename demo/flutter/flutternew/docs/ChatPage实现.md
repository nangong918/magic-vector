**ChatPage实现**
====



## 基本构思

* UI
  * 主页面：输入框，发送按钮，聊天记录滚动下拉容器
  * 自定义view：消息item（发送消息、接收消息）
  * 限制：未连接、未初始化、agent回复中不能发送消息


## 实现

业务应该从功能角度出发，所以先从最上层的UI绘制开始设计

### UI绘制


主页面绘制：
* 创建`Scaffold`空间
* 设置`AppBar`
* 创建body
  * 连接发送状态
    ```dart
    AnimatedBuilder(
        // 动画源是viewModel ChangeNotifier
        animation: _viewModel,
        builder: (BuildContext context, _) {
          // ChangeNotifier获取状态数据，动态监听
          final statusText = _buildStateText(_viewModel.state);
          return Container(
            ...
          );
        },
      ),
    ```
  * 聊天记录滚动下拉容器
    ```dart
      Expanded(
        // 动态聊天记录容器构建
        child: AnimatedBuilder(
          animation: _viewModel,
          builder: (BuildContext context, _) {
            // ChangeNotifier获取数据
            final messages = _viewModel.messages;
            // 聊天列表
            return ListView.builder(
              controller: _scrollController, // 滚动控制器
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12), // 内边距
              itemCount: messages.length, // 列表项数量
              itemBuilder: (BuildContext context, int index) {
                // ChatBubbleItem：自定义的聊天气泡组件
                return ChatBubbleItem(message: messages[index]);
              },
            );
          },
        ),
      ),
    ```
  * 输入框
    ```dart
      Expanded(
        child: TextField(
          // TextField：文本输入框，类似于Android中的EditText
          controller: _inputController, // 文本控制器
          maxLines: 5, // 最大行数
          minLines: 1, // 最小行数
          onChanged: (_) => setState(() {}), // 文本变化时更新UI
          decoration: InputDecoration(
            // InputDecoration：输入框装饰，类似于Android中的EditText的hint、background等属性
            // Android XML: android:hint="输入消息" 等属性
            hintText: _viewModel.isReceiving ? 'AI回复中，请稍候...' : '输入消息', // 提示文本
            filled: true, // 是否填充背景
            fillColor: Colors.grey.shade100, // 填充颜色
            border: OutlineInputBorder(
              // OutlineInputBorder：带边框的输入框样式
              borderRadius: BorderRadius.circular(24), // 圆角半径
              borderSide: BorderSide.none, // 无边框
            ),
            contentPadding: const EdgeInsets.symmetric(
              // 内容内边距
              horizontal: 16,
              vertical: 12,
            ),
          ),
        ),
      ),
    ```

* 自定义view
```dart
// ChatBubbleItem：聊天气泡组件，用于显示单条聊天消息
// 类似于Android中的自定义消息气泡布局
// Android XML: 自定义布局文件
// Jetpack Compose: 自定义Composable函数
class ChatBubbleItem extends StatelessWidget {
  const ChatBubbleItem({super.key, required this.message});

  final ChatMessage message;

  @override
  Widget build(BuildContext context) {
    final isUser = message.role == ChatRole.user; // 判断是否为用户消息
    final bubbleColor = const Color(0xFFE8F5E9); // 气泡颜色
    final align = isUser ? CrossAxisAlignment.end : CrossAxisAlignment.start; // 对齐方式

    // Padding：内边距组件
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4), // 垂直内边距
      child: Row(
        // Row：水平布局组件
        crossAxisAlignment: CrossAxisAlignment.start, // 子组件顶部对齐
        mainAxisAlignment: isUser ? MainAxisAlignment.end : MainAxisAlignment.start, // 主轴对齐方式
        children: [
          // 如果是AI消息，显示头像
          if (!isUser) ...[
            // CircleAvatar：圆形头像组件，类似于Android中的CircleImageView
            // Android XML: <de.hdodenhof.circleimageview.CircleImageView>
            // Jetpack Compose: CircleAvatar() 组件
            const CircleAvatar(
              radius: 15, // 半径
              backgroundColor: Color(0xFFF48FB1), // 背景颜色
              child: Icon(Icons.smart_toy, color: Colors.white, size: 16), // 图标
            ),
            const SizedBox(width: 6), // 间距
          ],
          
          // Flexible：灵活布局组件，类似于Android中的layout_weight
          // 确保聊天气泡不会超出屏幕宽度
          Flexible(
            child: Column(
              // Column：垂直布局组件
              crossAxisAlignment: align, // 对齐方式
              children: [
                // Container：容器组件，用于构建聊天气泡
                Container(
                  constraints: BoxConstraints(
                    // 约束最大宽度为屏幕宽度的78%
                    maxWidth: MediaQuery.of(context).size.width * 0.78,
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8), // 内边距
                  decoration: BoxDecoration(
                    // BoxDecoration：容器装饰，类似于Android中的background属性
                    // Android XML: android:background="@drawable/bubble_shape"
                    // Jetpack Compose: Modifier.background() 修饰符
                    color: bubbleColor, // 背景颜色
                    borderRadius: BorderRadius.only(
                      // 圆角半径，根据是否为用户消息显示不同的圆角
                      topLeft: const Radius.circular(24),
                      topRight: const Radius.circular(24),
                      bottomLeft: Radius.circular(isUser ? 24 : 0),
                      bottomRight: Radius.circular(isUser ? 0 : 24),
                    ),
                  ),
                  child: ValueListenableBuilder<String>(
                    // ValueListenableBuilder：值监听构建器，当textNotifier变化时重新构建
                    // 类似于Android中的ValueAnimator或LiveData观察
                    // Android XML: 使用DataBinding
                    // Jetpack Compose: 使用remember和LaunchedEffect等
                    valueListenable: message.textNotifier, // 监听的ValueNotifier
                    builder: (context, String value, child) {
                      return Text(
                        value, // 消息文本
                        style: const TextStyle(
                          color: Color(0xFF263238), // 文本颜色
                          fontSize: 16, // 字体大小
                          height: 1.4, // 行高
                        ),
                      );
                    },
                  ),
                ),
                
                const SizedBox(height: 4), // 间距
                
                // 时间文本
                Text(
                  _formatTime(message.createdAt), // 格式化时间
                  style: const TextStyle(
                    fontSize: 11, // 字体大小
                    color: Colors.black45, // 文本颜色
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // 格式化时间的静态方法
  static String _formatTime(DateTime dateTime) {
    final hh = dateTime.hour.toString().padLeft(2, '0'); // 小时，补前导零
    final mm = dateTime.minute.toString().padLeft(2, '0'); // 分钟，补前导零
    return '$hh:$mm'; // 返回格式化的时间字符串
  }
}
```

### 发送ws消息

page 点击事件 发送消息
```dart
  Future<void> _send() async {
    final text = _inputController.text.trim();
    // 清空输入框
    _inputController.clear();

    // 调用ViewModel发送消息
    await _viewModel.sendMessage(text);
  }
```

viewModel 参数校验 + 状态设置 发送消息
```dart
  Future<void> sendMessage(String input) async {
    final message = input.trim();
    
    // 设置状态为发送中
    _setState(const RealtimeChatState.recordingAndSending());
    
    // 添加用户消息到消息列表
    final userMessage = _addMessage(ChatRole.user, message);
    // 添加用户消息到历史记录
    _history.add(<String, String>{'role': 'user', 'content': userMessage.text});

    // 添加空的助手消息到消息列表，用于接收AI回复
    final assistantMessage = _addMessage(ChatRole.assistant, '');
    // 设置状态为接收中
    _setState(const RealtimeChatState.receiving());

    try {
      // 调用聊天服务发送消息
      await _chatService.sendChat(
        systemPrompt: _systemPrompt,
        history: List<Map<String, String>>.from(_history),
        userMessage: message,
        // 监听AI回复并处理AI回复的增量文本
        onDelta: (String deltaText) {
          assistantMessage.textNotifier.value =
              '${assistantMessage.textNotifier.value}$deltaText';
        },
        // 处理消息发送完成
        onDone: () {
          // 添加助手消息到历史记录
          _history.add(<String, String>{
            'role': 'assistant',
            'content': assistantMessage.text,
          });
        },
      );
      // 消息发送完成，设置状态为已连接
      _setState(const RealtimeChatState.initializedConnected());
    } catch (e) {
      // 消息发送失败，设置错误信息和状态
      _latestError = '消息发送失败: $e';
      _setState(RealtimeChatState.error(_latestError));
      // 错误处理完成后，设置状态为已连接
      _setState(const RealtimeChatState.initializedConnected());
    }
  }
```

service 发送消息
```dart
  Future<void> sendChat({
    required String systemPrompt, // 系统提示词，用于指导AI的行为
    required List<Map<String, String>> history, // 聊天历史记录
    required String userMessage, // 用户消息
    required void Function(String deltaText) onDelta, // 处理AI回复的增量文本的回调
    required void Function() onDone, // 消息处理完成的回调
  }) async {

    // ...
    
    // 构建请求体
    final requestBody = _buildRequestBody(
      systemPrompt: systemPrompt,
      history: history,
      userMessage: userMessage,
    );
    // 发送请求到服务器
    socket.add(jsonEncode(requestBody));

    try {
      // 等待消息处理完成
      await doneCompleter.future;
    } finally {
      // 关闭WebSocket连接
      await socket.close();
    }
  }
```


### 接收ws消息 + 渲染到view上

定义接收消息的livedata
```dart
class ChatMessage {
  ChatMessage({
    required this.id,
    required this.role,
    required this.createdAt,
    required String text,
  }) : textNotifier = ValueNotifier<String>(text);

  final String id;
  final ChatRole role;
  final DateTime createdAt;
  // valueNotifier: 值监听器，用于监听值变化，相当于Android中的LiveData
  final ValueNotifier<String> textNotifier;

  String get text => textNotifier.value;

  void dispose() {
    textNotifier.dispose();
  }
}
```

接收ws消息
```dart
  int _handleEvent(
    String event, { // 服务器发送的事件数据
    required void Function(String deltaText) onDelta, // 处理增量文本的回调函数
  }) {
    // 解析事件JSON数据
    final Map<String, dynamic> jsonMap =
        jsonDecode(event) as Map<String, dynamic>;

    // ...
    
    // 提取payload部分
    final payload = (jsonMap['payload'] as Map?)?.cast<String, dynamic>();
    // 提取choices部分
    final choices = (payload?['choices'] as Map?)?.cast<String, dynamic>();
    // 提取text列表
    final textList = choices?['text'] as List<dynamic>? ?? <dynamic>[];

    // 遍历text列表，提取content并调用回调函数
    for (final dynamic item in textList) {
      final itemMap = (item as Map?)?.cast<String, dynamic>();
      final content = itemMap?['content'] as String? ?? '';
      if (content.isNotEmpty) {
        // 调用回调
        onDelta(content);
      }
    }
    
    // ...
  
  }
```

消息会被获取传递到：onDelta然后回显
UI回显

```dart
// 处理AI回复的增量文本
onDelta: (String deltaText) {
  // 相当于livedata的postValue
  assistantMessage.textNotifier.value =
      '${assistantMessage.textNotifier.value}$deltaText';
},
```

渲染到view上：
view已经设置好了监听，当value发生变化就会渲染在view上：
```dart
child: ValueListenableBuilder<String>(
  valueListenable: message.textNotifier, // 监听的ValueNotifier
  builder: (context, String value, child) {
    return Text(
      value, // 消息文本
      style: const TextStyle(
        color: Color(0xFF263238), // 文本颜色
        fontSize: 16, // 字体大小
        height: 1.4, // 行高
      ),
    );
  },
),
```









