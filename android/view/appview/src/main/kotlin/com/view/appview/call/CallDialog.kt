package com.view.appview.call

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.FragmentActivity
import com.core.baseutil.image.ImageLoadUtil
import com.data.domain.constant.VadChatState
import com.view.appview.R
import com.view.appview.databinding.CallDialogBinding
import java.util.concurrent.atomic.AtomicBoolean

class CallDialog(
    private val fragmentActivity: FragmentActivity,
    private val callAo: CallAo
) {
    private val TAG = CallDialog::class.simpleName
    private val dialog: Dialog = Dialog(fragmentActivity)
    private val binding: CallDialogBinding = CallDialogBinding.inflate(
        LayoutInflater.from(fragmentActivity), null, false
    )
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
    // 是否正在通话
    val isCalling: AtomicBoolean = AtomicBoolean(false)

    init {
        initView()

        setListener()
    }

    @SuppressLint("ResourceType")
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
        // 关闭mic
        binding.ivMic.setImageResource(R.xml.mic_off_24px)
    }

    @SuppressLint("ResourceType")
    private fun setListener(){
        binding.vClose.setOnClickListener {
            // 不为空调用外部逻辑
            callAo.onCallEndClickRunnable?.run()
            // 内部逻辑
            dialog.dismiss()
            setIsCloseMic(true)
        }

        // mic
        binding.ivMic.setOnClickListener {
            // 不为空调用外部逻辑 (关闭、开启mic等逻辑)
            callAo.onMuteClickRunnable?.run()
            // 内部逻辑
            changeMicState()
//            binding.ivMic.setBackgroundResource(
//                if (isStopRecordAudio) {
//                    // 停止录音：展示开始录音
//                    R.drawable.round_grey_400
//                }
//                else {
//                    // 开始录音：展示停止录音
//                    R.drawable.round_grey_600
//                }
//            )
        }
        // call end
        binding.ivCallEnd.setOnClickListener {
            // 不为空调用外部逻辑
            callAo.onCallEndClickRunnable?.run()
            // 内部逻辑
            dialog.dismiss()
            setIsCloseMic(true)
        }
    }

    // 是否关闭mic
    private var isCloseMic = false
    private var vadChatState = VadChatState()

    fun setVadChatState(state: VadChatState){
        vadChatState = state

        // 关闭mic之后就不存在Speaking和Silent了
        if (isCloseMic){
            if (vadChatState == VadChatState.Silent || vadChatState == VadChatState.Speaking){
                vadChatState = VadChatState.Muted
            }
        }

        when (state) {
            is VadChatState.Muted -> {
                mainHandler.post {
                    binding.tvChatState.text = fragmentActivity.getString(R.string.muted)
                }
            }
            is VadChatState.Silent -> {
                mainHandler.post {
                    binding.tvChatState.text = fragmentActivity.getString(R.string.silent)
                }
            }
            is VadChatState.Speaking -> {
                mainHandler.post {
                    binding.tvChatState.text = fragmentActivity.getString(R.string.user_speaking)
                }
            }
            is  VadChatState.Replying -> {
                mainHandler.post {
                    binding.tvChatState.text = fragmentActivity.getString(R.string.agent_replying)
                }
            }
            is VadChatState.Error -> {
                mainHandler.post {
                    binding.tvChatState.text = fragmentActivity.getString(R.string.error)
                }
                Log.e(TAG, "setVadChatState: ${state.message}")
            }
        }
    }

    fun setChatMessage(message: String){
        mainHandler.post {
            binding.tvChatMessage.text = message
        }
    }

    fun getIsCloseMic(): Boolean {
        return isCloseMic
    }

    @SuppressLint("ResourceType")
    fun setIsCloseMic(isStopRecordAudio: Boolean) {
        // value
        isCloseMic = isStopRecordAudio
        // UI
        binding.ivMic.setImageResource(
            if (isCloseMic) {
                // 停止录音：展示开始录音
                R.xml.mic_24px
            }
            else {
                // 开始录音：展示停止录音
                R.xml.mic_off_24px
            }
        )
        // logic
        if (isCloseMic) {
            setVadChatState(VadChatState.Muted)
        }
        else {
            setVadChatState(VadChatState.Silent)
        }
    }

    fun changeMicState(){
        setIsCloseMic(!isCloseMic)
    }

    fun show() {
        dialog.show()
        isCalling.set(true)
    }

    fun dismiss() {
        dialog.dismiss()
        isCalling.set(false)
    }

}