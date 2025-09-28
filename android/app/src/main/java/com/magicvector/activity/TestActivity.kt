package com.magicvector.activity


import com.magicvector.databinding.ActivityTestBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.TestVm

class TestActivity : BaseAppCompatVmActivity<ActivityTestBinding, TestVm>(
    TestActivity::class,
    TestVm::class
) {
    override fun initBinding(): ActivityTestBinding {
        return ActivityTestBinding.inflate(layoutInflater)
    }

    override fun initViewModel() {
        super.initViewModel()
    }

    override fun setListener() {
        super.setListener()

        binding.btnSendMessage.setOnClickListener {
            vm.sendQuestion()
        }
    }

}