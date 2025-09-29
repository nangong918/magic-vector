package com.magicvector.activity

import android.os.Bundle
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
    }
}