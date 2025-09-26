package com.magicvector.utils

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.core.baseutil.BaseAppCompatActivity
import com.core.baseutil.ViewModelUtil
import com.magicvector.viewModel.base.ApiViewModelFactory
import kotlin.reflect.KClass

abstract class BaseAppCompatVmActivity<VB : ViewBinding, VM : ViewModel> (
    activityClassType: KClass<out FragmentActivity>,
    private val vmClassType: KClass<VM>
) : BaseAppCompatActivity<VB>(activityClassType){

    protected open lateinit var vm: VM

    override fun initViewModel() {
        val apiViewModelFactory = ApiViewModelFactory(
        )

        vm = ViewModelUtil.newViewModel(this, apiViewModelFactory, vmClassType.java)
    }
}