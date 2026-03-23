package com.magicvector.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.core.baseutil.ui.ToastUtils
import com.magicvector.domain.constant.BaseConstant
import com.magicvector.databinding.ActivityAgentInfoBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.AgentInfoVm
import kotlinx.coroutines.launch

class AgentInfoActivity : BaseAppCompatVmActivity<ActivityAgentInfoBinding, AgentInfoVm>(
    AgentInfoActivity::class,
    AgentInfoVm::class
) {
    override fun initBinding(): ActivityAgentInfoBinding {
        return ActivityAgentInfoBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initViewModel() {
        super.initViewModel()

        vm.initResource(this, binding.imvgAvatar)

        val agentId = intent.getStringExtra("agentId")
        vm.aao.agentId = agentId

        if (TextUtils.isEmpty(vm.aao.agentId)){
            ToastUtils.showToastActivity(
                this,
                getString(com.view.appview.R.string.agent_is_not_found)
            )
            finish()
        }

        NetworkLoadUtils.showDialog(this)
        lifecycleScope.launch {
            runCatching {
                vm.requestAgentInfo(agentId!!)
            }.onFailure {
                Log.e(TAG, "getAgentInfo error: ", it)
                NetworkLoadUtils.dismissDialogSafety(this@AgentInfoActivity)
            }.onSuccess {
                NetworkLoadUtils.dismissDialogSafety(this@AgentInfoActivity)
            }
        }

        observeData()
    }

    fun observeData() {
        binding.gEditName.setLiveData(vm.aao.nameLd)
        binding.gEditSetting.setLiveData(vm.aao.descriptionLd)
    }

    override fun initView() {
        super.initView()

        binding.topBar.setTitle(getString(com.view.appview.R.string.set_agent))

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

        // 修改
        binding.btnConfirm.setOnClickListener {
            ToastUtils.showToastActivity(
                this,
                getString(com.view.appview.R.string.under_development)
            )
        }
        // 删除
        binding.btnDelete.setOnClickListener {
            ToastUtils.showToastActivity(
                this,
                getString(com.view.appview.R.string.under_development)
            )
        }
    }


}