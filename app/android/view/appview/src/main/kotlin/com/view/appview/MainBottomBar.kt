package com.view.appview

import android.content.Context
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.data.domain.OnPositionItemClick
import com.view.appview.databinding.ViewMainBottomBarBinding

class MainBottomBar : ConstraintLayout {

    constructor(context: Context) : super(context) {
        init(context)
    }
    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs) {
        init(context)
    }
    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private var binding: ViewMainBottomBarBinding = ViewMainBottomBarBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private lateinit var clickLys: Array<LinearLayout>
    private lateinit var imageViews: Array<ImageView>

    private fun init(context: Context){
        clickLys = arrayOf(
            binding.lyClick1, binding.lyClick2, binding.lyClick3
        )
        imageViews = arrayOf(
            binding.imgvHome, binding.imgvApply, binding.imgvMine
        )

        updateUi(0)
    }

    fun setSelected(position: MainSelectItemEnum) {
        updateUi(position.position)
    }

    private fun updateUi(clickPosition: Int) {
        val isClick = BooleanArray(imageViews.size)
        isClick[clickPosition] = true
        for (i in imageViews.indices) {

            val color: Int = if (isClick[i])
                R.color.s1_300 else
                R.color.s1_800
            imageViews[i].setColorFilter(
                ContextCompat.getColor(context, color),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    fun setBaseBarColor(@ColorRes colorResId: Int) {
        binding.lyMain.setBackgroundResource(
            colorResId
        )
    }

    fun clickListener(click: OnPositionItemClick) {
        for (i in clickLys.indices) {
            val finalI = i
            clickLys[i].setOnClickListener { v: View ->
                updateUi(finalI)
                click.onPositionItemClick(finalI)
            }
        }
    }

}