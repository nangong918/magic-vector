package com.magicvector.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
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
import com.magicvector.domain.model.message.MessageContactItemModel
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
import com.magicvector.service.ChatService
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.viewModel.activity.MainEffect
import com.magicvector.viewModel.activity.MainIntent
import com.magicvector.viewModel.activity.MainVm
import com.magicvector.viewModel.base.ApiViewModelFactory
import com.magicvector.ui.view.activity.MainActivityScreen
import com.magicvector.utils.activity.BaseComponentActivity
import com.view.appview.MainSelectItemEnum

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

    private var chatService: ChatService? = null

    private val serviceConnection = object : ServiceConnection {
        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val binder = service as ChatService.ChatServiceBinder
            chatService = binder.getService()

            val handler = binder.getChatMessageHandler()
            vm.processIntent(MainIntent.ChatServiceBound(handler))
        }

        @RequiresPermission(Manifest.permission.RECORD_AUDIO)
        override fun onServiceDisconnected(name: ComponentName?) {
            vm.processIntent(MainIntent.ChatServiceUnbound)
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

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun unbindAndStopChatService() {
        if (vm.dataState.value.isChatServiceBound) {
            unbindService(serviceConnection)
        }
        val intent = Intent(this, ChatService::class.java)
        stopService(intent)
        chatService = null
        vm.processIntent(MainIntent.ChatServiceUnbound)
    }

    private fun openChatPage(ao: MessageContactItemModel) {
        val intentAo = ChatIntentAo().apply { this.ao = ao }
        val intent = Intent(this, ComposeAgentChatActivity::class.java).apply {
            putExtra(ChatIntentAo::class.simpleName, intentAo)
        }
        startActivity(intent)
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

    @SuppressLint("MissingPermission")
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
}