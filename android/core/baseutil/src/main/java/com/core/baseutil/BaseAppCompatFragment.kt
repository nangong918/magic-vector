package com.core.baseutil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KClass

abstract class BaseAppCompatFragment<VB : ViewBinding>(
    fragmentClassType: KClass<out Fragment>
) : Fragment(){

    protected lateinit var binding: VB
    abstract fun initBinding(): VB
    protected val tag = fragmentClassType.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = initBinding()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViewModel()

        // 此处binding才生效
        initView()

        setListener()
    }

    protected open fun initViewModel(){

    }

    protected open fun initView(){

    }

    protected open fun setListener() {

    }

}