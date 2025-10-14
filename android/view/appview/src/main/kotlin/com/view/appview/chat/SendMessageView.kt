package com.view.appview.chat

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.view.appview.databinding.ViewSendMessageBinding

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
}