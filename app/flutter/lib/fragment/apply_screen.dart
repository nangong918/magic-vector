// lib/screens/apply_screen.dart
import 'package:flutter/material.dart';

class ApplyScreen extends StatelessWidget {
  const ApplyScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Padding(
      padding: EdgeInsets.all(20.0),
      child: Column(
        children: [
          Text('Main - Apply', style: TextStyle(fontSize: 18)),
          SizedBox(height: 12),
          Text('Flutter placeholder page.'),
        ],
      ),
    );
  }
}