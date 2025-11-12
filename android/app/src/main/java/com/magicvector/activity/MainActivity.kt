package com.magicvector.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.baseutil.fragmentActivity.BaseAppCompatActivity
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.data.domain.OnPositionItemClick
import com.magicvector.callback.OnCreateAgentCallback
import com.magicvector.databinding.ActivityMainBinding
import com.magicvector.fragment.MessageListFragment
import com.magicvector.fragment.MineFragment
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.MainVm
import com.magicvector.viewModel.fragment.MessageListVm
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

        fragmentMap.get(MainSelectItemEnum.HOME.position)?.let {
            if (it is MessageListFragment){
                initCreateAgentLuncher(it)
            }
            else {
                Log.w(TAG, "initFragment::当前Fragment不是MessageListFragment")
            }
        }
    }

    override fun initView() {
        super.initView()
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

        binding.mainBottomBar.clickListener(object : OnPositionItemClick {
            override fun onPositionItemClick(position: Int) {
                try {
                    currentSelected = MainSelectItemEnum.getItem(position)?: MainSelectItemEnum.HOME
                } catch (e: Exception) {
                    Log.e(TAG, "initFragment::获取intent的初始化数据错误: ", e)
                }
                changeFragment()
            }
        })
    }

    //------------------------Fragment------------------------

    /**
     * 缓存的Fragment
     */
    private val fragmentMap = SparseArray<Fragment?>(MainSelectItemEnum.entries.size)

    private var fragmentManager: FragmentManager? = null
    private var currentSelected: MainSelectItemEnum = MainSelectItemEnum.HOME
    private var lastSelected: MainSelectItemEnum? = null

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

    /**
     * 初始化Fragment
     */
    private fun initFragment() {
        fragmentManager = supportFragmentManager

        currentSelected = MainSelectItemEnum.HOME
        try {
            if (intent.hasExtra(MainSelectItemEnum.INTENT_EXTRA_NAME)) {
                currentSelected =
                    intent.getSerializableExtra(MainSelectItemEnum.INTENT_EXTRA_NAME) as MainSelectItemEnum
            }
        } catch (e : Exception) {
            Log.e(TAG, "initFragment::获取intent的初始化数据错误: ", e)
        }

        changeFragment()
    }

    /**
     * 切换Fragment
     */
    private fun changeFragment() {
        if (currentSelected === lastSelected) {
            Log.i(TAG, "changeFragment::当前Fragment和上一次Fragment一致")
            return
        }
        when (currentSelected) {
            MainSelectItemEnum.HOME -> {
//                setStatusBarColor(
//                    com.view.appview.R.color.green_90
//                )
                Log.i(TAG, "changeFragment::切换到HOME")
                this.setBaseBarColorRes(com.view.appview.R.color.s1_10)
                turnToTargetFragment(MainSelectItemEnum.HOME, MessageListFragment::class.java, null)
            }
            MainSelectItemEnum.APPLY -> {}
            MainSelectItemEnum.MINE -> {
//                setStatusBarColor(
//                    com.view.appview.R.color.green_90
//                )
                this.setBaseBarColorRes(com.view.appview.R.color.s1_10)
                turnToTargetFragment(MainSelectItemEnum.MINE, MineFragment::class.java, null)
            }
        }
    }

    /**
     * 切换目标fragment
     */
    fun turnToTargetFragment(
        fragmentType: MainSelectItemEnum,
        clazz: Class<out Fragment?>,
        args: Bundle?
    ) {
        binding.mainBottomBar.setSelected(fragmentType)

        var newFragment = fragmentMap.get(fragmentType.position)

        if (newFragment == null) {
            try {
                // 如果没有参数，使用无参构造函数
                newFragment = clazz.getConstructor().newInstance()
                fragmentMap.put(fragmentType.position, newFragment)
            } catch (e: NoSuchMethodException) {
                Log.e(TAG, "No such constructor", e)
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "Error creating fragment", e)
            }
        }

        if (newFragment != null) {
            // 如果需要，可以为 Fragment 设置参数
            if (args != null) {
                newFragment.setArguments(args)
            }

            // 使用Add替代replace、Navigation
            val transaction: FragmentTransaction = fragmentManager!!.beginTransaction()
            transaction.replace(binding.fragmentContainer.id, newFragment)
            transaction.commit()

            // 缓存
            lastSelected = currentSelected
        }
    }

    /**
     * 设置底栏颜色
     */
    fun setBaseBarColorRes(@ColorRes colorResId: Int) {
        binding.mainBottomBar.setBaseBarColor(colorResId)
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
    }
}