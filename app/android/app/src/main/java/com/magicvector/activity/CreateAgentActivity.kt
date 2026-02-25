package com.magicvector.activity

import android.content.Intent
import android.os.Bundle
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.core.baseutil.ui.ToastUtils
import com.data.domain.constant.BaseConstant
import com.magicvector.databinding.ActivityCreateAgentBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.CreateAgentVm

class CreateAgentActivity : BaseAppCompatVmActivity<ActivityCreateAgentBinding, CreateAgentVm>(
    CreateAgentActivity::class,
    CreateAgentVm::class
) {
    override fun initBinding(): ActivityCreateAgentBinding {
        return ActivityCreateAgentBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initViewModel() {
        super.initViewModel()

        vm.initResource(this, binding.imvgAvatar)

        observeData()
    }

    fun observeData() {
        binding.gEditName.setLiveData(vm.aao.nameLd)
        binding.gEditSetting.setLiveData(vm.aao.descriptionLd)
    }

    override fun initView() {
        super.initView()

        binding.topBar.setTitle(getString(com.view.appview.R.string.create_agent))

        binding.gEditName.setMaxLine(1)
        binding.gEditName.setMaxNumber(BaseConstant.Constant.MAX_AGENT_NAME_LENGTH)

        binding.gEditName.setTitle(getString(com.view.appview.R.string.agent_name))
        binding.gEditName.setHint(getString(com.view.appview.R.string.agent_name_hint))
        binding.gEditSetting.setTitle(getString(com.view.appview.R.string.agent_setting))
        binding.gEditSetting.setHint(getString(com.view.appview.R.string.agent_setting_hint))
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
                    if (vm.aao.isCreateSuccess){
                        val resultIntent = Intent().apply {
                            putExtra(CreateAgentActivity::class.simpleName, true) // 设置返回值
                        }
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    }
                    // 创建失败弹窗
                    ToastUtils.showToastActivity(this@CreateAgentActivity,
                        getString(com.view.appview.R.string.create_failed))
                }
            })
        }
    }
}