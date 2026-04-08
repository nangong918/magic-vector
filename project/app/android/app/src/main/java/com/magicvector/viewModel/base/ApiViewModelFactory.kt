package com.magicvector.viewModel.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ApiViewModelFactory : ViewModelProvider.Factory {

    val tag = ApiViewModelFactory::class.simpleName

    constructor(){}

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return try {
            // 获取 ViewModel 的构造器，参数为 ApiRequestImpl 和 SocketMessageSender
            val constructor = modelClass.getConstructor()
            // 创建实例并返回
            constructor.newInstance() as T
        } catch (e: Exception) {
            Log.e(tag, "Unknown ViewModel class: ${modelClass.name}", e)
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}", e)
        }
    }

}