package com.view.appview.chat

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.view.appview.databinding.ViewSendMessageBinding
import kotlinx.coroutines.Runnable

class SendMessageView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        init(context)
    }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private var binding: ViewSendMessageBinding = ViewSendMessageBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private fun init(context: Context) {
        // 初始化view
        binding.btnTakeAudio.text = context.getString(com.view.appview.R.string.press_to_record)

        // 点击事件
        binding.btnAudio.setOnClickListener {
            setKeyboardOpen(false)
        }
        binding.btnKeyboard.setOnClickListener {
            setKeyboardOpen(true)
        }
    }

    fun getEditText(): EditText {
        return binding.edtv
    }

    fun getEditMessage(): String {
        return binding.edtv.text.toString()
    }

    fun setEditMessage(message: String?) {
        binding.edtv.setText(message ?: "")
    }

    fun setSendClickListener(listener: OnClickListener?) {
        listener?.let {
            binding.btnSend.setOnClickListener(listener)
        }
    }

    fun setImgClickListener(listener: OnClickListener?) {
        listener?.let {
            binding.btnPicture.setOnClickListener(listener)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setTakAudioOnTouchListener(startRecording: Runnable, stopRecording: Runnable){
        binding.btnTakeAudio.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.btnTakeAudio.text = context.getString(com.view.appview.R.string.release_to_cancel)
                    startRecording.run()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    binding.btnTakeAudio.text = context.getString(com.view.appview.R.string.press_to_record)
                    stopRecording.run()
                    true
                }
                else -> false
            }
        }
    }

    private var isKeyboardOpen = true

    fun setKeyboardOpen(isOpen: Boolean) {
        isKeyboardOpen = isOpen
        if (isOpen){
            binding.lyKeyboard.visibility = VISIBLE
            binding.lyAudio.visibility = GONE
        }
        else {
            binding.lyKeyboard.visibility = GONE
            binding.lyAudio.visibility = VISIBLE
        }
    }

    private var isEnableSend = true
    fun setIsEnableSend(isEnable: Boolean) {
        isEnableSend = isEnable
        binding.btnPicture.let {
            it.setBackgroundResource(
                if (isEnable) com.view.appview.R.drawable.background_chat_input
                else com.view.appview.R.drawable.background_chat_not_input
            )
            it.isEnabled = isEnable
        }
        binding.btnSend.let {
            it.setBackgroundResource(
                if (isEnable) com.view.appview.R.drawable.background_chat_input
                else com.view.appview.R.drawable.background_chat_not_input
            )
            it.isEnabled = isEnable
        }
        binding.btnTakeAudio.let {
            it.setBackgroundResource(
                if (isEnable) com.view.appview.R.drawable.round_corners_button2
                else com.view.appview.R.drawable.round_corners_not_button2
            )
            it.isEnabled = isEnable
        }
    }
    fun getIsEnableSend(): Boolean {
        return isEnableSend
    }
}