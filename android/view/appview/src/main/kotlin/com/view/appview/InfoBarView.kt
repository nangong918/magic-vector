package com.view.appview

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.view.appview.databinding.ViewInfoBarBinding

class InfoBarView : ConstraintLayout {

    constructor(context: Context) : super(context) {
    }
    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs) {
    }
    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    private var binding: ViewInfoBarBinding = ViewInfoBarBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun setBack(onClickListener: OnClickListener?) {
        onClickListener?.let {
            binding.imgvBack.setOnClickListener(onClickListener)
        }
    }

    fun setTitle(title: String?) {
        binding.tvTitle.text = title?:""
    }

}