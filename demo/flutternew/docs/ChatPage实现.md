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













