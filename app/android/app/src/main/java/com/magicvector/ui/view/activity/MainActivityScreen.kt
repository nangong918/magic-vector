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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.data.domain.ao.message.MessageContactItemAo
import com.magicvector.fragment.AgentEditorOverlay
import com.magicvector.fragment.MessageListScreen
import com.magicvector.fragment.MineScreen
import com.magicvector.manager.network.NetworkState
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.AgentListEvent
import com.magicvector.viewModel.activity.MainState
import com.view.appview.MainSelectItemEnum
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MainActivityScreen(
    state: MainState,
    onSelectTab: (MainSelectItemEnum) -> Unit,
    onCreateAgent: () -> Unit,
    onOpenAgentEditor: (String) -> Unit,
    onOpenChat: (MessageContactItemAo) -> Unit,
    onEditorClose: () -> Unit,
    onEditorNameChange: (String) -> Unit,
    onEditorDescriptionChange: (String) -> Unit,
    onEditorSubmit: () -> Unit,
    onEditorDelete: () -> Unit,
    agentListEventFlow: SharedFlow<AgentListEvent>,
    networkStateFlow: StateFlow<NetworkState>,
) {
    val backgroundColor = remember { Color(0xFFF6F7F8) }
    var refreshToken by remember { mutableLongStateOf(0L) }
    val latestCreate = rememberUpdatedState(onCreateAgent)
    val latestEditor = rememberUpdatedState(onOpenAgentEditor)
    val networkState by networkStateFlow.collectAsState()

    LaunchedEffect(agentListEventFlow) {
        agentListEventFlow.collect {
            refreshToken = System.currentTimeMillis()
        }
    }
    LaunchedEffect(networkState.isNetworkOnline, networkState.isWsConnected) {
        if (networkState.isNetworkOnline) {
            refreshToken = System.currentTimeMillis()
        }
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
                    isServiceBound = state.isChatServiceBound,
                    onCreateAgentClick = { latestCreate.value.invoke() },
                    refreshToken = refreshToken,
                    onOpenChat = onOpenChat,
                    onOpenAgentEditor = { latestEditor.value.invoke(it) }
                )
                MainSelectItemEnum.MEDIA -> MessageListScreen(
                    isServiceBound = state.isChatServiceBound,
                    onCreateAgentClick = { latestCreate.value.invoke() },
                    refreshToken = refreshToken,
                    onOpenChat = onOpenChat,
                    onOpenAgentEditor = { latestEditor.value.invoke(it) }
                )
                MainSelectItemEnum.MINE -> MineScreen()
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
            state = MainState(currentSelected = MainSelectItemEnum.HOME, isChatServiceBound = true),
            onSelectTab = {},
            onCreateAgent = {},
            onOpenAgentEditor = {},
            onOpenChat = {},
            onEditorClose = {},
            onEditorNameChange = {},
            onEditorDescriptionChange = {},
            onEditorSubmit = {},
            onEditorDelete = {},
            agentListEventFlow = MutableSharedFlow(),
            networkStateFlow = MutableStateFlow(NetworkState(isNetworkOnline = true, isWsConnected = false))
        )
    }
}
