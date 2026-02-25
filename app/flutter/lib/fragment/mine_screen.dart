// lib/screens/mine_screen.dart
import 'package:flutter/material.dart';

class MineScreen extends StatelessWidget {
  final bool isServiceBound;

  const MineScreen({
    super.key,
    required this.isServiceBound,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20.0),
      child: Column(
        children: [
          const Text('Main - Mine', style: TextStyle(fontSize: 18)),
          const SizedBox(height: 12),
          Text(
            isServiceBound ? 'Realtime service ready' : 'Realtime service unavailable',
            style: TextStyle(
              color: isServiceBound ? Colors.green : Colors.grey,
            ),
          ),
          const SizedBox(height: 20),
          ElevatedButton(
            onPressed: () {
              // 测试按钮
              Navigator.pushNamed(context, '/test');
            },
            child: const Text('测试跳转'),
          ),
        ],
      ),
    );
  }
}