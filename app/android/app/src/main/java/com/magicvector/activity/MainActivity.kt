package com.magicvector.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.magicvector.domain.bo.AgentChatBO
import com.magicvector.domain.constant.MainSelectEnum
import com.magicvector.service.ChatService
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.MainEffect
import com.magicvector.viewModel.activity.MainIntent
import com.magicvector.viewModel.activity.MainVm
import com.magicvector.viewModel.base.ApiViewModelFactory
import com.magicvector.ui.view.activity.MainActivityScreen
import com.magicvector.utils.activity.BaseComponentActivity

/**
 * 启动首页
 */
class MainActivity : BaseComponentActivity() {

    private val vm: MainVm by viewModels { ApiViewModelFactory() }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vm.processIntent(MainIntent.Initialize(parseInitialSelection()))
        bindChatService()

        setContent {
            MagicVectorTheme {
                val state by vm.uiState.collectAsState()
                val dataState by vm.dataState.collectAsState()
                LaunchedEffect(Unit) {
                    vm.effect.collect { effect ->
                        when (effect) {
                            is MainEffect.ShowToast -> {
                                Toast.makeText(this@MainActivity, effect.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                MainActivityScreen(
                    state = state,
                    dataState = dataState,
                    messageListVm = vm.messageListVm,
                    controlVm = vm.controlVm,
                    mineVm = vm.mineVm,
                    onSelectTab = { vm.processIntent(MainIntent.SelectTab(it)) },
                    onCreateAgent = { vm.processIntent(MainIntent.OpenCreateAgent) },
                    onOpenAgentEditor = { vm.processIntent(MainIntent.OpenEditAgent(it)) },
                    onOpenChat = { openChatPage(it) },
                    onEditorClose = { vm.processIntent(MainIntent.CloseAgentEditor) },
                    onEditorNameChange = { vm.processIntent(MainIntent.UpdateEditorName(it)) },
                    onEditorDescriptionChange = { vm.processIntent(MainIntent.UpdateEditorDescription(it)) },
                    onEditorSubmit = { vm.processIntent(MainIntent.SubmitAgentEditor) },
                    onEditorDelete = { vm.processIntent(MainIntent.DeleteAgent) }
                )
            }
        }
    }

    //------------------------Service------------------------

    private val serviceConnection = object : ServiceConnection {
        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val binder = service as ChatService.ChatServiceBinder
            // ✅ 只通过 Intent 传递给 ViewModel，Activity 不持有
            vm.processIntent(MainIntent.ChatServiceBound(
                chatController = binder.getChatController()
            ))
        }

        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        override fun onServiceDisconnected(name: ComponentName?) {
            // 仅在 Service 进程被系统杀死时调用，正常解绑不会触发
            vm.processIntent(MainIntent.ChatServiceUnbound)
        }
    }

    // ✅ 直接 bindService，不需要 startService 和检查运行状态
    private fun bindChatService() {
        val intent = Intent(this, ChatService::class.java)
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    // ✅ 解绑时只需要 unbindService，不需要 stopService
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun unbindChatService() {
        if (vm.dataState.value.isChatServiceBound) {
            try {
                unbindService(serviceConnection)
                vm.processIntent(MainIntent.ChatServiceUnbound)
            } catch (e: Exception) {
                Log.e(TAG, "unbindService error: ", e)
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

        fun startWithSelection(context: Context, selection: MainSelectEnum) {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainSelectEnum::class.simpleName, selection)
            }
            context.startActivity(intent)
        }

        // 页面跳转
        @Suppress("unused")
        fun startWithHome(context: Context) = startWithSelection(context, MainSelectEnum.AGENT)
        @Suppress("unused")
        fun startWithMine(context: Context) = startWithSelection(context, MainSelectEnum.MINE)
    }

    private fun openChatPage(agentBo: AgentChatBO) {
        val intent = Intent(this, AgentChatActivity::class.java).apply {
            putExtra(AgentChatBO::class.simpleName, agentBo)
        }
        startActivity(intent)
    }

    private fun parseInitialSelection(): MainSelectEnum {
        return try {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(MainSelectEnum::class.simpleName) as? MainSelectEnum
                ?: MainSelectEnum.AGENT
        } catch (e: Exception) {
            Log.e(TAG, "parseInitialSelection: error", e)
            MainSelectEnum.AGENT
        }
    }

    //------------------------lifecycle------------------------

    override fun onStart() {
        super.onStart()
        bindChatService()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onStop() {
        super.onStop()
        unbindChatService()
    }

}