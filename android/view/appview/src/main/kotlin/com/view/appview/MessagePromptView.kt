package com.view.appview

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.magicvector.appview.databinding.ViewMessagePromptBinding

class MessagePromptView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        init(context)
    }
    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs) {
        init(context)
    }
    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private var binding: ViewMessagePromptBinding = ViewMessagePromptBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private fun init(context: Context){
        setMessageNum(currentNum)
    }

    private var currentNum = 0

    fun setMessageNum(num: Int) {
        currentNum = num
        if (num <= 0) {
            binding.imgvBackground.visibility = GONE
            binding.tvMessageNum.visibility = GONE
        } else if (num < 100) {
            binding.imgvBackground.visibility = VISIBLE
            binding.tvMessageNum.visibility = VISIBLE
            binding.tvMessageNum.text = num.toString()
        } else {
            binding.imgvBackground.visibility = VISIBLE
            binding.tvMessageNum.visibility = VISIBLE
            val numStr = "99+"
            binding.tvMessageNum.text = numStr
        }
    }

    fun getCurrentNum(): Int {
        return currentNum
    }
}