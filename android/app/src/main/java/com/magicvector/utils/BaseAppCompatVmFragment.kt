package com.czy.smartmedicine.utils


import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding
import com.core.baseutil.fragmentActivity.BaseAppCompatFragment
import com.core.baseutil.viewModel.ViewModelUtil
import com.magicvector.viewModel.base.ApiViewModelFactory
import kotlin.reflect.KClass

abstract class BaseAppCompatVmFragment<VB : ViewBinding, VM : ViewModel>(
    fragmentClassType: KClass<out Fragment>,
    private val vmClassType: KClass<VM>,
) : BaseAppCompatFragment<VB>(fragmentClassType) {

    protected open lateinit var vm: VM

    override fun initViewModel() {
        val apiViewModelFactory = ApiViewModelFactory(
        )
        vm = ViewModelUtil.newViewModel(this, apiViewModelFactory, vmClassType.java)
    }
}