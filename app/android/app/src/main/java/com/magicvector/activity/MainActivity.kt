package com.magicvector.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.magicvector.callback.OnCreateAgentCallback
import com.magicvector.databinding.ActivityMainBinding
import com.magicvector.R
import com.magicvector.service.ChatService
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.MainVm
import com.view.appview.MainSelectItemEnum

class MainActivity : BaseAppCompatVmActivity<ActivityMainBinding, MainVm>(
    MainActivity::class,
    MainVm::class
) {
    override fun initBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Example of a call to a native method
//        binding.tvHello.text = stringFromJNI()

        initFragment()
    }

    override fun initView() {
        super.initView()
    }

    override fun initViewModel() {
        super.initViewModel()

        // 绑定服务
        bindChatService()
    }

    override fun initWindow() {
        super.initWindow()

//        setStatusBarColor(
//            android.R.color.white
//        )

        val layoutParams = binding.statusBar.layoutParams
        layoutParams.height = getStatusBarHeight()
        binding.statusBar.layoutParams = layoutParams
        Log.i("TAG", "initWindow::statusBarHeight: ${binding.statusBar.layoutParams.height}")
    }

    override fun setListener() {
        super.setListener()
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

            // 连接成功后使用
            val handler = binder.getChatMessageHandler()
            vm.realtimeChatController = handler
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            chatService = null
        }
    }

    // todo 需要检查Service是否已经启动了，如果没有启动Service就启动service
    private fun bindChatService() {
        val intent = Intent(this, ChatService::class.java)

        // 先启动服务（保证后台运行）
        startService(intent)
        // 再绑定服务（获取Binder接口）
        bindService(intent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun unbindAndStopChatService() {
        unbindService(serviceConnection)
        val intent = Intent(this, ChatService::class.java)
        stopService(intent)
        isBound = false
        chatService = null
    }

    //------------------------Fragment------------------------

    private var currentSelected: MainSelectItemEnum = MainSelectItemEnum.HOME

    var createAgentLauncher: ActivityResultLauncher<Intent>? = null

    fun turnToCreateAgent() {
        val intent = Intent(this, CreateAgentActivity::class.java)
        createAgentLauncher?.launch(intent)
    }

    // activity launcher必须要在LifecycleOwner 的状态为 STARTED 或更早的状态时进行注册
    fun initCreateAgentLuncher(createAgentCallback: OnCreateAgentCallback){
        createAgentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // 如果intent 返回值中包括ok，则表明创建成功，需要进行刷新list
            val backIntent: Intent? = result.data
            if (backIntent != null) {
                val createResult: Boolean = backIntent.getBooleanExtra(
                    CreateAgentActivity::class.simpleName,
                    false
                )
                // 创建成功?
                createAgentCallback.onCreateAgent(createResult)
            }
        }
    }

    private lateinit var navController: NavController
    /**
     * 初始化Fragment  Navigation Compose
     */
    private fun initFragment() {
        currentSelected = MainSelectItemEnum.HOME
        try {
            if (intent.hasExtra(MainSelectItemEnum.INTENT_EXTRA_NAME)) {
                currentSelected =
                    intent.getSerializableExtra(MainSelectItemEnum.INTENT_EXTRA_NAME) as MainSelectItemEnum
            }
        } catch (e : Exception) {
            Log.e(TAG, "initFragment::获取intent的初始化数据错误: ", e)
            currentSelected = MainSelectItemEnum.HOME
        }

        changeFragment()
    }

    /**
     * 切换Fragment
     */
    private fun changeFragment() {
        navController = findNavController(R.id.fragment_main_activity_main)

        val graph = navController.navInflater.inflate(R.navigation.main_navigation)
        val start = when (currentSelected) {
            MainSelectItemEnum.HOME -> R.id.nagi_messageList
            MainSelectItemEnum.APPLY -> R.id.nagi_messageList
            MainSelectItemEnum.MINE -> R.id.nagi_mine
        }
        graph.setStartDestination(start)
        navController.graph = graph

        binding.navViewMain.setupWithNavController(navController)
    }


    /**
     * 导航到目标页面
     */
    fun navigateTo(destinationId: Int, args: Bundle? = null) {
        try {
            if (args != null) {
                navController.navigate(destinationId, args)
            } else {
                navController.navigate(destinationId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "导航失败: $destinationId", e)
        }
    }

    /**
     * 处理返回键
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * 获取当前 NavController
     */
    fun getNavController(): NavController {
        return navController
    }


    /**
     * 调用Cpp的JNI方法
     */
    external fun stringFromJNI(): String

    /**
     * 初始化，加载JNI的Cpp库
     */
    companion object {
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

        fun startWithHome(context: Context) = startWithSelection(context, MainSelectItemEnum.HOME)
        fun startWithMine(context: Context) = startWithSelection(context, MainSelectItemEnum.MINE)
    }

    //------------------------lifecycle------------------------

    override fun onDestroy() {
        super.onDestroy()
        unbindAndStopChatService()
    }
}