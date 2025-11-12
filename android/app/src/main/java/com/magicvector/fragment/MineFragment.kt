package com.magicvector.fragment


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.magicvector.utils.BaseAppCompatVmFragment
import com.magicvector.activity.test.TestActivity
import com.magicvector.databinding.FragmentMineBinding
import com.magicvector.viewModel.fragment.MineVm


class MineFragment : BaseAppCompatVmFragment<FragmentMineBinding, MineVm>(
    MineFragment::class,
    MineVm::class
) {
    override fun initBinding(): FragmentMineBinding {
        return FragmentMineBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun initViewModel() {
        super.initViewModel()
    }

    override fun setListener() {
        super.setListener()

        binding.btnTest.setOnClickListener {
            val intent = Intent(activity, TestActivity::class.java)
            startActivity(intent)
        }
    }

}