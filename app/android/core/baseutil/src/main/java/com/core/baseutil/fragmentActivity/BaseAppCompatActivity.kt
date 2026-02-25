package com.core.baseutil.fragmentActivity


import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.size
import androidx.fragment.app.FragmentActivity
import androidx.viewbinding.ViewBinding
import com.core.baseutil.StatusBarView
import java.util.Optional
import java.util.function.Consumer
import kotlin.reflect.KClass


/**
 * 通用的AppCompatActivity基础类
 * 解决了：1.binding
 * 2.Tag
 * 3.顶部沉浸式bar
 * 4.自动关闭输入法
 */
abstract class BaseAppCompatActivity<VB : ViewBinding>(
    activityClassType: KClass<out FragmentActivity>
) : AppCompatActivity(){

    protected lateinit var binding: VB

    abstract fun initBinding(): VB
    protected val TAG = activityClassType.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.binding = initBinding()
        setContentView(binding.root)

        initViewModel()

        initView()

        setListener()
    }

    protected open fun initViewModel(){

    }

    protected open fun initView(){

    }

    protected open fun setListener() {

    }

    private var statusBarColorId = R.color.transparent

    protected open fun initWindow() {
        //去除标题导航栏


//        //去除时间和电量等
//        getWindow().setFlags(
//                WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN
//        );


        // 隐藏标题导航栏
        supportActionBar?.hide()

        // 隐藏状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // 隐藏导航栏
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setStatusBarColor(statusBarColorId)
    }

    @SuppressLint("ResourceType")
    fun setStatusBarColor(colorId: Int) {
        statusBarColorId = colorId
        // 获取 DecorView
        val decorView = window.decorView as ViewGroup
        val count = decorView.size
        if (count > 0 && decorView.getChildAt(count - 1) is StatusBarView) {
            decorView.getChildAt(count - 1).setBackgroundResource(statusBarColorId)
        }
        else {
            // 创建并添加 StatusBarView
            val statusBarView = createStatusBarView(this, statusBarColorId)
            decorView.addView(statusBarView)
        }

        // 获取根视图并设置窗口插图
        val rootView = (findViewById<View>(R.id.content) as ViewGroup).getChildAt(0) as ViewGroup
        rootView.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
//            val statusBarHeight = insets.getSystemWindowInsetTop()
            val statusBarHeight = getStatusBarHeight()
            v.setPadding(0, statusBarHeight, 0, 0) // 设置顶部填充以适应状态栏
            insets
        }
    }

    fun getStatusBarHeight() : Int {
        return getStatusBarHeight(this)
    }


    // 创建 StatusBarView
    private fun createStatusBarView(activity: Activity, colorId: Int): StatusBarView {
        val statusBarView = StatusBarView(activity)
        val params =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                getStatusBarHeight(activity)
            )
        statusBarView.layoutParams = params
        statusBarView.setBackgroundResource(colorId)
        return statusBarView
    }

    // 获取状态栏高度
    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    private fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        closeKeyBoard()
        return super.onTouchEvent(event)
    }

    // 点击其他位置关闭输入框
    fun closeKeyBoard() {
        if (currentFocus != null && currentFocus?.windowToken != null) {
            val v = currentFocus
            closeSoftInput(this, v)
        }
    }

    // 关闭键盘输入法
    fun closeSoftInput(context: Context, v: View?) {
        if (v != null) {
            val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        initWindow()
    }
}