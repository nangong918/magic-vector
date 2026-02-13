import 'package:flutter/material.dart';

import '../viewmodel/chat_view_model.dart';

class ChatPage extends StatefulWidget {
  final String title;
  final String? description;

  const ChatPage({
    super.key,
    required this.title,
    this.description,
  });

  @override
  State<ChatPage> createState() => _ChatPageState();
}


class _ChatPageState extends State<ChatPage> {
  final ChatViewModel _viewModel = ChatViewModel();
  final TextEditingController _inputController = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    // 添加ViewModel监听器，当ViewModel状态变化时触发_onViewModelChanged
    _viewModel.addListener(_onViewModelChanged);
    // 初始化ViewModel，加载系统提示词等
    _viewModel.initialize();
  }

  /// ViewModel状态变化时的回调方法
  /// 主要功能：
  /// 1. 当有新消息时，自动滚动到底部
  /// 2. 当出现错误时，显示错误提示
  void _onViewModelChanged() {
    // 使用addPostFrameCallback确保在UI构建完成后执行滚动
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_scrollController.hasClients) {
        return;
      }
      // 滚动到底部，+80是为了留出一些空间，确保最新消息完全可见
      _scrollController.animateTo(
        _scrollController.position.maxScrollExtent + 80,
        duration: const Duration(milliseconds: 220),
        curve: Curves.easeOut,
      );
    });
    // 当ViewModel状态为错误时，显示错误提示
    if (mounted && _viewModel.state.status == RealtimeChatStatus.error) {
      final message = _viewModel.latestError;
      if (message.isNotEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(message)),
        );
      }
    }
  }

  @override
  void dispose() {
    _viewModel.removeListener(_onViewModelChanged);
    _viewModel.dispose();
    _inputController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  /// 发送消息的方法
  /// 主要功能：
  /// 1. 检查输入是否为空和是否可以发送
  /// 2. 清空输入框
  /// 3. 调用ViewModel发送消息
  /// 4. 更新UI状态
  Future<void> _send() async {
    final text = _inputController.text.trim();
    // 检查输入是否为空和是否可以发送（ViewModel状态是否为已连接）
    if (text.isEmpty || !_viewModel.canSend) {
      return;
    }
    // 清空输入框
    _inputController.clear();
    // 触发UI更新，显示输入框已清空
    setState(() {});
    // 调用ViewModel发送消息
    await _viewModel.sendMessage(text);
    // 发送完成后，确保页面仍然存在，然后更新UI
    if (mounted) {
      setState(() {});
    }
  }

  /// 根据ViewModel状态构建状态文本
  /// 用于在聊天页面顶部显示当前连接状态
  String _buildStateText(RealtimeChatState state) {
    switch (state.status) {
      case RealtimeChatStatus.notInitialized:
        return '未初始化';
      case RealtimeChatStatus.initializing:
        return '初始化中...';
      case RealtimeChatStatus.initializedConnected:
        return '已连接';
      case RealtimeChatStatus.recordingAndSending:
        return '发送中...';
      case RealtimeChatStatus.receiving:
        return 'AI回复中...';
      case RealtimeChatStatus.disconnected:
        return '已断开';
      case RealtimeChatStatus.error:
        return state.message == null ? '错误' : '错误: ${state.message}';
    }
  }

  @override
  Widget build(BuildContext context) {
    // 计算是否可以发送消息：ViewModel状态为已连接且输入框不为空
    final canSendNow = _viewModel.canSend && _inputController.text.trim().isNotEmpty;
    
    // Scaffold：Flutter中的基础布局组件，类似于Android中的DrawerLayout或AppCompatActivity的根布局
    // Android XML: <DrawerLayout> 或 <RelativeLayout> 作为根布局
    // Jetpack Compose: Scaffold() 组件
    return Scaffold(
      // AppBar：应用栏，类似于Android中的ActionBar或Toolbar
      // Android XML: <androidx.appcompat.widget.Toolbar>
      // Jetpack Compose: TopAppBar() 组件
      appBar: AppBar(
        title: Text(widget.title), // 标题文本
        backgroundColor: const Color(0xFFF48FB1), // 背景颜色
        foregroundColor: Colors.white, // 前景颜色（文本和图标）
      ),
      
      // body：页面的主要内容区域
      body: Column(
        // Column：垂直布局组件，类似于Android中的LinearLayout(orientation="vertical")
        // Android XML: <LinearLayout android:orientation="vertical">
        // Jetpack Compose: Column() 组件
        children: [
          // 连接状态、发送状态
          // AnimatedBuilder：动画构建器，当ViewModel变化时重新构建子组件
          // 类似于Android中的DataBinding或LiveData观察
          // Android XML: 使用DataBinding表达式
          // Jetpack Compose: 使用remember和LaunchedEffect等
          AnimatedBuilder(
            animation: _viewModel, // 动画源，这里是ViewModel
            builder: (BuildContext context, _) {
              final statusText = _buildStateText(_viewModel.state);
              // Container：容器组件，类似于Android中的View或ViewGroup
              // Android XML: <View> 或 <LinearLayout>
              // Jetpack Compose: Box() 或 Container() 组件
              return Container(
                width: double.infinity, // 宽度充满父容器
                color: Colors.pink.shade50, // 背景颜色
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8), // 内边距
                child: Text(
                  statusText, // 状态文本
                  style: TextStyle(
                    fontSize: 12, // 字体大小
                    color: _viewModel.state.status == RealtimeChatStatus.error
                        ? Colors.red // 错误状态为红色
                        : Colors.black54, // 其他状态为灰色
                  ),
                ),
              );
            },
          ),
          
          // Expanded：扩展组件，占据剩余空间
          // 类似于Android中的LinearLayout的weight属性
          // Android XML: <LinearLayout android:layout_weight="1">
          // Jetpack Compose: Modifier.weight(1f) 修饰符
          Expanded(
            // 动态聊天记录容器构建
            child: AnimatedBuilder(
              animation: _viewModel,
              builder: (BuildContext context, _) {
                final messages = _viewModel.messages;
                // 聊天列表
                // ListView.builder：列表视图，懒加载构建子项
                // 类似于Android中的RecyclerView
                // Android XML: <androidx.recyclerview.widget.RecyclerView>
                // Jetpack Compose: LazyColumn() 组件
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

          // 消息发送框
          // SafeArea：安全区域组件，避免内容被系统UI遮挡
          // 类似于Android中的WindowInsetsCompat
          // Android XML: 使用android:fitsSystemWindows="true"
          // Jetpack Compose: Modifier.systemBarsPadding() 修饰符
          SafeArea(
            top: false, // 顶部不需要安全区域
            child: Padding(
              // Padding：内边距组件
              // 类似于Android中的View的padding属性
              // Android XML: android:padding="16dp"
              // Jetpack Compose: Modifier.padding() 修饰符
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 10), // 左、上、右、下内边距
              child: Row(
                // Row：水平布局组件，类似于Android中的LinearLayout(orientation="horizontal")
                // Android XML: <LinearLayout android:orientation="horizontal">
                // Jetpack Compose: Row() 组件
                crossAxisAlignment: CrossAxisAlignment.end, // 子组件底部对齐
                children: [
                  // Expanded：扩展组件，占据剩余空间
                  Expanded(
                    child: TextField(
                      // TextField：文本输入框，类似于Android中的EditText
                      // Android XML: <EditText>
                      // Jetpack Compose: OutlinedTextField() 或 TextField() 组件
                      controller: _inputController, // 文本控制器
                      maxLines: 5, // 最大行数
                      minLines: 1, // 最小行数
                      onChanged: (_) => setState(() {}), // 文本变化时更新UI
                      decoration: InputDecoration(
                        // InputDecoration：输入框装饰，类似于Android中的EditText的hint、background等属性
                        // Android XML: android:hint="输入消息" 等属性
                        // Jetpack Compose: TextField的placeholder、label等参数
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
                  
                  // SizedBox：固定大小的空间组件，类似于Android中的Space
                  // Android XML: <Space android:width="8dp" />
                  // Jetpack Compose: Spacer(modifier = Modifier.width(8.dp))
                  const SizedBox(width: 8),

                  // 发送按钮
                  // Material：材质组件，提供触摸反馈
                  // 类似于Android中的MaterialButton
                  // Android XML: <com.google.android.material.button.MaterialButton>
                  // Jetpack Compose: Button() 或 ElevatedButton() 组件
                  Material(
                    color: canSendNow ? const Color(0xFFF48FB1) : Colors.grey.shade400, // 背景颜色
                    borderRadius: BorderRadius.circular(20), // 圆角半径
                    child: InkWell(
                      // InkWell：可点击的组件，提供水波纹效果
                      // 类似于Android中的View.OnClickListener
                      // Android XML: android:onClick="@onClick"
                      // Jetpack Compose: Modifier.clickable {}
                      borderRadius: BorderRadius.circular(20), // 圆角半径
                      onTap: canSendNow ? _send : null, // 点击回调
                      child: const Padding(
                        padding: EdgeInsets.all(12), // 内边距
                        child: Icon(Icons.send, color: Colors.white, size: 20), // 发送图标
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

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