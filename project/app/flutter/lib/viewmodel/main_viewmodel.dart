import 'package:equatable/equatable.dart';
import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';




/// viewModel

final mainViewModelProvider = StateNotifierProvider<MainViewModel, MainState>((ref) {
  return MainViewModel();
});


class MainViewModel extends StateNotifier<MainState> {

  MainViewModel() : super(const MainState());

  // Effect Stream
  final _effectController = StreamController<MainEffect>.broadcast();
  Stream<MainEffect> get effect => _effectController.stream;

  // 保留对外控制器引用
  dynamic realtimeChatController;

  @override
  void dispose() {
    _effectController.close();
    super.dispose();
  }

  // 处理事件（对应 Compose 的 processIntent）
  void onEvent(MainEvent event) {
    if (event is InitializeEvent) {
      _handleInitialize(event);
    } else if (event is SelectTabEvent) {
      _handleSelectTab(event);
    } else if (event is ChatServiceBoundEvent) {
      _handleChatServiceBound(event);
    } else if (event is ChatServiceUnboundEvent) {
      _handleChatServiceUnbound();
    } else if (event is OpenCreateAgentEvent) {
      _handleOpenCreateAgent();
    }
  }

  void _handleInitialize(InitializeEvent event) {
    state = state.copyWith(currentTab: event.initialTab);
  }

  void _handleSelectTab(SelectTabEvent event) {
    state = state.copyWith(currentTab: event.tab);
  }

  void _handleChatServiceBound(ChatServiceBoundEvent event) {
    realtimeChatController = event.handler;
    state = state.copyWith(isChatServiceBound: true);
  }

  void _handleChatServiceUnbound() {
    realtimeChatController = null;
    state = state.copyWith(isChatServiceBound: false);
  }

  void _handleOpenCreateAgent() {
    // 发送 Effect
    _effectController.add(LaunchCreateAgentEffect());
  }
}



/// statue
enum MainTab { home, apply, mine }

class MainState extends Equatable {
  final MainTab currentTab;
  final bool isChatServiceBound;

  const MainState({
    this.currentTab = MainTab.home,
    this.isChatServiceBound = false,
  });

  MainState copyWith({
    MainTab? currentTab,
    bool? isChatServiceBound,
  }) {
    return MainState(
      currentTab: currentTab ?? this.currentTab,
      isChatServiceBound: isChatServiceBound ?? this.isChatServiceBound,
    );
  }

  @override
  List<Object?> get props => [currentTab, isChatServiceBound];
}

// Intent/Event 定义
sealed class MainEvent {}

class InitializeEvent extends MainEvent {
  final MainTab initialTab;
  InitializeEvent(this.initialTab);
}

class SelectTabEvent extends MainEvent {
  final MainTab tab;
  SelectTabEvent(this.tab);
}

class ChatServiceBoundEvent extends MainEvent {
  final dynamic handler; // 根据你的 RealtimeChatController 类型调整
  ChatServiceBoundEvent(this.handler);
}

class ChatServiceUnboundEvent extends MainEvent {}

class OpenCreateAgentEvent extends MainEvent {}

// Effect 定义
sealed class MainEffect {}

class LaunchCreateAgentEffect extends MainEffect {}






