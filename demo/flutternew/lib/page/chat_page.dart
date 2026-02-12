import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

// 聊天页面
class ChatPage extends StatefulWidget  { // StatefulWidget能保存状态，Stateless不能保存状态。
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

  // 1. 定义状态变量保存txt内容，初始值设为加载中
  String _aiPrompt = '';
  bool _isLoadFailed = false;

  // 2. 读取txt文件的核心方法
  Future<String> loadTxtFile() async {
    try {
      String content = await rootBundle.loadString('assets/txt/clt_agent.txt');
      return content;
    } catch (e) {
      return '读取失败：$e';
    }
  }

  // 3. 读取赋值到state并更新ui
  void _initLoadTxt() async {
    String result = await loadTxtFile();
    // 更新状态变量（必须用setState触发UI刷新）
    setState(() {
      if (result.startsWith('读取失败')) {
        _isLoadFailed = true;
      }
      _aiPrompt = result;
    });
  }

  // 4. 页面初始化时异步读取并更新状态
  @override
  void initState() {
    super.initState();
    // 页面启动就执行读取操作
    _initLoadTxt();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title), // 注意：State类中通过widget访问父类属性
        backgroundColor: const Color(0xFFF48FB1),
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              widget.title,
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
            ),
            if (widget.description != null) ...[
              const SizedBox(height: 16),
              Text(
                widget.description!,
                style: TextStyle(
                  fontSize: 16,
                  color: Colors.grey[700],
                ),
              ),
            ],
            const SizedBox(height: 32),
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(12),
                ),
                // 这里展示读取到的txt内容（替换原来的空Center）
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Text(
                    _aiPrompt, // 你的状态变量，需提前在State类中定义
                    style: TextStyle(
                      fontSize: 15,
                      color: _isLoadFailed ? Colors.red : Colors.black87, // 失败标红
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // 示例：点击按钮可以使用保存的_aiPrompt变量
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('当前Prompt：${_aiPrompt.substring(0, 20)}...'), // 截取展示
              duration: const Duration(seconds: 1),
            ),
          );
        },
        backgroundColor: const Color(0xFFF48FB1),
        child: const Icon(Icons.send, color: Colors.white),
      ),
    );
  }
}






