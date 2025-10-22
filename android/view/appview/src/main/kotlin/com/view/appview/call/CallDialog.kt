package com.view.appview.call

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import com.core.baseutil.image.ImageLoadUtil
import com.data.domain.constant.VadChatState
import com.view.appview.R
import com.view.appview.databinding.CallDialogBinding

class CallDialog(
    private val fragmentActivity: FragmentActivity,
    private val callAo: CallAo
) {
    private val TAG = CallDialog::class.simpleName
    private val dialog: Dialog = Dialog(fragmentActivity)
    private val binding: CallDialogBinding = CallDialogBinding.inflate(
        LayoutInflater.from(fragmentActivity), null, false
    )

    init {
        initView()

        setListener()
    }

    private fun initView(){
        dialog.setContentView(binding.root)
        dialog.setCancelable(false)

        val window = dialog.window
        if (window != null) {
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            // 设置对话框填充满整个屏幕
            val layoutParams = window.attributes
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            window.attributes = layoutParams
        }

        // view
        callAo.let {
            binding.tvTitle.text = it.agentName?:""
            it.agentAvatar?.let { agentAvatar ->
                ImageLoadUtil.loadImageViewByResource(
                    agentAvatar,
                    binding.imvgAvatar
                )
            }
        }

        binding.tvChatState.text = fragmentActivity.getString(R.string.muted)
    }

    @SuppressLint("ResourceType")
    private fun setListener(){
        binding.vClose.setOnClickListener {
            dialog.dismiss()
        }

        // mic
        binding.ivMic.setOnClickListener {
            // 不为空调用外部逻辑
            callAo.onMuteClickRunnable?.run()
            // 内部逻辑
            binding.ivMic.setImageResource(
                if (isStopRecordAudio) {
                    // 停止录音：展示开始录音
                    R.xml.mic_24px
                }
                else {
                    // 开始录音：展示停止录音
                    R.xml.mic_off_24px
                }
            )
            binding.ivMic.setBackgroundResource(
                if (isStopRecordAudio) {
                    // 停止录音：展示开始录音
                    R.drawable.round_grey_400
                }
                else {
                    // 开始录音：展示停止录音
                    R.drawable.round_grey_600
                }
            )
        }
        // call end
        binding.ivCallEnd.setOnClickListener {
            // 不为空调用外部逻辑
            callAo.onCallEndClickRunnable?.run()
            // 内部逻辑
            dialog.dismiss()
        }
    }

    private var isStopRecordAudio = false
    private var vadChatState = VadChatState()

    fun setVadChatState(state: VadChatState){
        vadChatState = state
        when (state) {
            is VadChatState.Muted -> {
                binding.tvChatState.text = fragmentActivity.getString(R.string.muted)
            }
            is VadChatState.Silent -> {
                binding.tvChatState.text = fragmentActivity.getString(R.string.silent)
            }
            is VadChatState.Speaking -> {
                binding.tvChatState.text = fragmentActivity.getString(R.string.user_speaking)
            }
            is  VadChatState.Replying -> {
                binding.tvChatState.text = fragmentActivity.getString(R.string.agent_replying)
            }
            is VadChatState.Error -> {
                binding.tvChatState.text = fragmentActivity.getString(R.string.error)
                Log.e(TAG, "setVadChatState: ${state.message}")
            }
        }
    }

    fun getIsStopRecordAudio(): Boolean {
        return isStopRecordAudio
    }

    fun show() {
        dialog.show()
    }

    fun dismiss() {
        dialog.dismiss()
    }

}