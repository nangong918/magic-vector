package com.magicvector.activity

import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.annotation.ColorRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.core.baseutil.fragmentActivity.BaseAppCompatActivity
import com.magicvector.databinding.ActivityMainBinding
import com.magicvector.fragment.MessageListFragment
import com.view.appview.MainSelectItemEnum

class MainActivity : BaseAppCompatActivity<ActivityMainBinding>(
    MainActivity::class
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

        setStatusBarColor(
            android.R.color.white
        )
    }

    //------------------------Fragment------------------------

    /**
     * 缓存的Fragment
     */
    private val fragmentMap = SparseArray<Fragment?>(MainSelectItemEnum.entries.size)

    private var fragmentManager: FragmentManager? = null
    private var currentSelected: MainSelectItemEnum = MainSelectItemEnum.HOME
    private var lastSelected: MainSelectItemEnum? = null

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
            return
        }
        when (currentSelected) {
            MainSelectItemEnum.HOME -> {
                setStatusBarColor(
                    com.view.appview.R.color.green_90
                )
                this.setBaseBarColorRes(com.view.appview.R.color.green_0)
                turnToTargetFragment(MainSelectItemEnum.HOME, MessageListFragment::class.java, null)
            }
            MainSelectItemEnum.APPLY -> {}
            MainSelectItemEnum.MINE -> {}
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
            transaction.replace(binding.fragmentContainer.getId(), newFragment)
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