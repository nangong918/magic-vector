// lib/screens/home_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class HomeScreen extends ConsumerWidget {
  final bool isServiceBound;
  final VoidCallback onCreateAgent;

  const HomeScreen({
    super.key,
    required this.isServiceBound,
    required this.onCreateAgent,
  });

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Text(
          'Main - Home (Riverpod + MVI)',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 12),
        Text(
          isServiceBound ? 'ChatService: connected' : 'ChatService: disconnected',
          style: TextStyle(
            color: isServiceBound ? Colors.green : Colors.red,
          ),
        ),
        const SizedBox(height: 16),
        ElevatedButton(
          onPressed: onCreateAgent,
          child: const Text('创建 Agent'),
        ),
        const SizedBox(height: 24),
        const Divider(),
        const SizedBox(height: 12),
        const Text('This screen is now fully rendered by Flutter.'),
      ],
    );
  }
}