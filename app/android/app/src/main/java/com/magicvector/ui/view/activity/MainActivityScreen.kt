package com.magicvector.ui.view.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.magicvector.MainApplication
import com.magicvector.domain.bo.AgentChatBO
import com.magicvector.fragment.AgentEditorOverlay
import com.magicvector.fragment.ControlScreen
import com.magicvector.fragment.MessageListScreen
import com.magicvector.fragment.MineScreen
import com.magicvector.manager.network.NetworkState
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.MainDataState
import com.magicvector.viewModel.activity.MainState
import com.magicvector.viewModel.fragment.ControlVm
import com.magicvector.viewModel.fragment.MessageListVm
import com.magicvector.viewModel.fragment.MineVm
import com.view.appview.MainSelectItemEnum
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun MainActivityScreen(
    state: MainState,
    dataState: MainDataState,
    messageListVm: MessageListVm,
    controlVm: ControlVm,
    mineVm: MineVm,
    onSelectTab: (MainSelectItemEnum) -> Unit,
    onCreateAgent: () -> Unit,
    onOpenAgentEditor: (Long) -> Unit,
    onOpenChat: (AgentChatBO) -> Unit,
    onEditorClose: () -> Unit,
    onEditorNameChange: (String) -> Unit,
    onEditorDescriptionChange: (String) -> Unit,
    onEditorSubmit: () -> Unit,
    onEditorDelete: () -> Unit,
) {
    val backgroundColor = remember { Color(0xFFF6F7F8) }
    // rememberUpdatedState: 副作用中安全使用最新回调
    val latestCreate = rememberUpdatedState(onCreateAgent)
    val latestEditor = rememberUpdatedState(onOpenAgentEditor)
    val networkStateFlow = if (LocalInspectionMode.current) {
        remember { MutableStateFlow(NetworkState(isNetworkOnline = true, isWsConnected = false)) }
    } else {
        MainApplication.getNetworkManager().state
    }
    val networkState by networkStateFlow.collectAsState()
    // messageList的网络状态
    LaunchedEffect(dataState.isChatServiceBound, networkState.isWsConnected) {
        messageListVm.processIntent(
            com.magicvector.viewModel.fragment.MessageListIntent.UpdateConnectionState(
                isServiceBound = dataState.isChatServiceBound,
                isWsConnected = networkState.isWsConnected
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().background(backgroundColor),
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.currentSelected == MainSelectItemEnum.HOME,
                    onClick = { onSelectTab(MainSelectItemEnum.HOME) },
                    icon = {
                        Icon(
                            painter = painterResource(id = com.view.appview.R.drawable.home_24px),
                            contentDescription = stringResource(id = com.view.appview.R.string.home_messagelist)
                        )
                    },
                    label = { Text(stringResource(id = com.view.appview.R.string.home_messagelist)) }
                )
                NavigationBarItem(
                    selected = state.currentSelected == MainSelectItemEnum.MEDIA,
                    onClick = { onSelectTab(MainSelectItemEnum.MEDIA) },
                    icon = {
                        Icon(
                            painter = painterResource(id = com.view.appview.R.drawable.settings_24px),
                            contentDescription = stringResource(id = com.view.appview.R.string.home_option)
                        )
                    },
                    label = { Text(stringResource(id = com.view.appview.R.string.home_option)) }
                )
                NavigationBarItem(
                    selected = state.currentSelected == MainSelectItemEnum.MINE,
                    onClick = { onSelectTab(MainSelectItemEnum.MINE) },
                    icon = {
                        Icon(
                            painter = painterResource(id = com.view.appview.R.drawable.person_24px),
                            contentDescription = stringResource(id = com.view.appview.R.string.home_mine)
                        )
                    },
                    label = { Text(stringResource(id = com.view.appview.R.string.home_mine)) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Spacer(
                modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars).fillMaxWidth()
            )
            when (state.currentSelected) {
                MainSelectItemEnum.HOME -> MessageListScreen(
                    viewModel = messageListVm,
                    onCreateAgentClick = { latestCreate.value.invoke() },
                    onOpenChat = onOpenChat,
                    onOpenAgentEditor = { latestEditor.value.invoke(it) }
                )
                MainSelectItemEnum.MEDIA -> ControlScreen(viewModel = controlVm)
                MainSelectItemEnum.MINE -> MineScreen(viewModel = mineVm)
            }
        }

        AgentEditorOverlay(
            state = state.agentEditor,
            onNameChange = onEditorNameChange,
            onDescriptionChange = onEditorDescriptionChange,
            onSubmit = onEditorSubmit,
            onDelete = onEditorDelete,
            onClose = onEditorClose
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun MainActivityScreenPreview() {
    MagicVectorTheme {
        MainActivityScreen(
            state = MainState(currentSelected = MainSelectItemEnum.HOME),
            dataState = MainDataState(isChatServiceBound = true),
            messageListVm = MessageListVm(),
            controlVm = ControlVm(),
            mineVm = MineVm(),
            onSelectTab = {},
            onCreateAgent = {},
            onOpenAgentEditor = {},
            onOpenChat = {},
            onEditorClose = {},
            onEditorNameChange = {},
            onEditorDescriptionChange = {},
            onEditorSubmit = {},
            onEditorDelete = {}
        )
    }
}
