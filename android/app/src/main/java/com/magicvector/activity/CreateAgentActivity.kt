package com.magicvector.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.core.baseutil.ui.ToastUtils
import com.magicvector.databinding.ActivityCreateAgentBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.CreateAgentVm

class CreateAgentActivity : BaseAppCompatVmActivity<ActivityCreateAgentBinding, CreateAgentVm>(
    CreateAgentActivity::class,
    CreateAgentVm::class
) {
// todo 尝试实现创建Agent
    override fun initBinding(): ActivityCreateAgentBinding {
        return ActivityCreateAgentBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initViewModel() {
        super.initViewModel()

        vm.initResource(this, binding.imvgAvatar)
    }

    override fun initView() {
        super.initView()

        binding.topBar.setTitle(getString(com.view.appview.R.string.create_agent))
    }

    override fun setListener() {
        super.setListener()

        binding.topBar.setBack(
            onClickListener = {
                finish()
            }
        )

        binding.imvgAvatar.setOnClickListener {
            vm.selectAgentAvatar(this)
        }

        binding.btnConfirm.setOnClickListener {
            NetworkLoadUtils.showDialog(this)
            vm.doCreateAgent(this, object : SyncRequestCallback{
                override fun onThrowable(throwable: Throwable?) {
                    NetworkLoadUtils.dismissDialogSafety(this@CreateAgentActivity)
                    ToastUtils.showToastActivity(
                        this@CreateAgentActivity,
                        getString(com.view.appview.R.string.create_failed)
                    )
                }

                override fun onAllRequestSuccess() {
                    NetworkLoadUtils.dismissDialogSafety(this@CreateAgentActivity)
                    val resultIntent = Intent().apply {
                        putExtra(CreateAgentActivity::class.simpleName, true) // 设置返回值
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            })
        }
    }
}