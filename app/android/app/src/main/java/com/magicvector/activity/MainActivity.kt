package com.magicvector.activity

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.magicvector.callback.OnCreateAgentCallback
import com.magicvector.fragment.MessageListScreen
import com.magicvector.fragment.MineScreen
import com.magicvector.service.ChatService
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.MainEffect
import com.magicvector.viewModel.activity.MainIntent
import com.magicvector.viewModel.activity.MainState
import com.magicvector.viewModel.activity.MainVm
import com.magicvector.viewModel.base.ApiViewModelFactory
import com.view.appview.MainSelectItemEnum
import kotlinx.coroutines.launch

/**
 * 启动首页
 */
class MainActivity : ComponentActivity() {

    private val vm: MainVm by viewModels { ApiViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initCreateAgentLauncher()

        vm.processIntent(MainIntent.Initialize(parseInitialSelection()))
        bindChatService()
        observeEffects()

        setContent {
            MagicVectorTheme {
                val state by vm.uiState.collectAsState()
                MainActivityScreen(
                    state = state,
                    onSelectTab = { vm.processIntent(MainIntent.SelectTab(it)) },
                    onCreateAgent = { vm.processIntent(MainIntent.OpenCreateAgent) }
                )
            }
        }
    }

    //------------------------Service------------------------

    private var chatService: ChatService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val binder = service as ChatService.ChatServiceBinder
            chatService = binder.getService()
            isBound = true

            val handler = binder.getChatMessageHandler()
            vm.processIntent(MainIntent.ChatServiceBound(handler))
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            vm.processIntent(MainIntent.ChatServiceUnbound)
            isBound = false
            chatService = null
        }
    }

    // 修正后的绑定方法：先检查，再启动/绑定 检查Service是否已经启动了，如果没有启动Service就启动service
    private fun bindChatService() {
        val intent = Intent(this, ChatService::class.java)

        // 检查服务是否已在运行
        if (!isServiceRunning(ChatService::class.java)) {
            // 服务未运行，使用 startService（不是 startForegroundService）
            startService(intent)  // 普通后台服务
        }

        // 绑定服务
        val flags = BIND_AUTO_CREATE or BIND_NOT_FOREGROUND
        bindService(intent, serviceConnection, flags)
    }

    // 辅助方法
    private fun <T> isServiceRunning(serviceClass: Class<T>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    private fun unbindAndStopChatService() {
        if (isBound) {
            unbindService(serviceConnection)
        }
        val intent = Intent(this, ChatService::class.java)
        stopService(intent)
        isBound = false
        chatService = null
        vm.processIntent(MainIntent.ChatServiceUnbound)
    }

    //------------------------Create Agent------------------------

    private var createAgentCallback: OnCreateAgentCallback? = null
    private lateinit var createAgentLauncher: ActivityResultLauncher<Intent>

    fun turnToCreateAgent() {
        vm.processIntent(MainIntent.OpenCreateAgent)
    }

    private fun initCreateAgentLauncher() {
        createAgentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val backIntent: Intent? = result.data
            if (backIntent != null) {
                val createResult: Boolean = backIntent.getBooleanExtra(
                    ComposeCreateAgentActivity::class.simpleName,
                    false
                )
                createAgentCallback?.onCreateAgent(createResult)
            }
        }
    }

    /**
     * 初始化，加载JNI的Cpp库
     */
    companion object {
        private const val TAG = "MainActivity"

        // Used to load the 'magicvector' library on application startup.
        init {
            System.loadLibrary("magicvector")
        }

        fun startWithSelection(context: Context, selection: MainSelectItemEnum) {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainSelectItemEnum.INTENT_EXTRA_NAME, selection)
            }
            context.startActivity(intent)
        }

        // 页面跳转
        @Suppress("unused")
        fun startWithHome(context: Context) = startWithSelection(context, MainSelectItemEnum.HOME)
        @Suppress("unused")
        fun startWithMine(context: Context) = startWithSelection(context, MainSelectItemEnum.MINE)
    }

    //------------------------lifecycle------------------------

    override fun onDestroy() {
        super.onDestroy()
        unbindAndStopChatService()
    }

    private fun parseInitialSelection(): MainSelectItemEnum {
        return try {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(MainSelectItemEnum.INTENT_EXTRA_NAME) as? MainSelectItemEnum
                ?: MainSelectItemEnum.HOME
        } catch (e: Exception) {
            Log.e(TAG, "parseInitialSelection: error", e)
            MainSelectItemEnum.HOME
        }
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        MainEffect.LaunchCreateAgent -> {
                            val intent = Intent(this@MainActivity, ComposeCreateAgentActivity::class.java)
                            createAgentLauncher.launch(intent)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun MainActivityScreen(
    state: MainState,
    onSelectTab: (MainSelectItemEnum) -> Unit,
    onCreateAgent: () -> Unit
) {
    val backgroundColor = remember {
        Color(0xFFF6F7F8)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        containerColor = backgroundColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.currentSelected == MainSelectItemEnum.HOME,
                    onClick = { onSelectTab(MainSelectItemEnum.HOME) },
                    {
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
                    {
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
                    {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(
                modifier = Modifier
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .fillMaxWidth()
            )
            when (state.currentSelected) {
                MainSelectItemEnum.HOME -> MessageListScreen(
                    isServiceBound = state.isChatServiceBound,
                    onCreateAgentClick = onCreateAgent
                )

                // 暂时未实现Media页面
                MainSelectItemEnum.MEDIA -> MediaScreen(
                    isServiceBound = state.isChatServiceBound,
                    onCreateAgent = onCreateAgent
                )

                MainSelectItemEnum.MINE -> MineScreen()
            }
        }
    }
}

@Composable
private fun MediaScreen(
    isServiceBound: Boolean,
    onCreateAgent: () -> Unit
) {
    MessageListScreen(
        isServiceBound = isServiceBound,
        onCreateAgentClick = onCreateAgent
    )
}



// 预览函数
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun GreetingPreview() {
    MagicVectorTheme {
        // 创建模拟的 State
        val mockState = MainState(
            currentSelected = MainSelectItemEnum.HOME,
            isChatServiceBound = true
        )
        MainActivityScreen(
            state = mockState,
            onSelectTab = { },
            onCreateAgent = { }
        )
    }
}