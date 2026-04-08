**flutterUI**
====




### flutter UI


StatefulWidget: 有状态组件 StatelessWidget: 无状态组件

如果要在组件内部存储状态值就需要使用StatefulWidget。否则使用StatelessWidget的话是没法存储状态值的。
存储状态的话需要实现`State<T extend StatefulWidget>`
例如：
```dart
class ChatPage extends StatefulWidget  { // StatefulWidget能保存状态，Stateless不能保存状态。
  
  // 不可变状态值
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
  
  // 存储的可变状态值
  String _aiPrompt = '';
  
  @override
  Widget build(BuildContext context) {
    return Scaffold();
  }
}
```

ValueNotifier: 类似Android的LiveData












