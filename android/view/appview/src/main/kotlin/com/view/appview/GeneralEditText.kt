package com.view.appview

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import com.view.appview.databinding.ViewGeneralEditTextBinding

class GeneralEditText : ConstraintLayout{

    constructor(context: Context) : super(context) {
    }
    constructor(context: Context, attrs: android.util.AttributeSet) : super(context, attrs) {
    }
    constructor(context: Context, attrs: android.util.AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    private var binding: ViewGeneralEditTextBinding = ViewGeneralEditTextBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }

    fun setHint(hint: String) {
        binding.edtvInfo.hint = hint
    }

    fun setHintColor(color: Int) {
        binding.edtvInfo.setHintTextColor(color)
    }

    fun getEditText(): EditText {
        return binding.edtvInfo
    }

    // 输入类型设置
    fun setOnlyNumber(isOnlyNum: Boolean) {
        binding.edtvInfo.inputType = if (isOnlyNum) {
            InputType.TYPE_CLASS_NUMBER
        } else {
            InputType.TYPE_CLASS_TEXT // 允许文本输入
        }
    }

    fun setOnlyNumberAndMaxNumber(isOnlyNum: Boolean, maxNumber: Int) {
        setOnlyNumber(isOnlyNum)
        setMaxNumber(maxNumber)
    }

    fun setMaxNumber(maxNumber: Int) {
        var finalMaxNumber = maxNumber
        if (finalMaxNumber <= 0) {
            finalMaxNumber = 1
        }
        getEditText().addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // 检查输入长度，如果超过最大值则截断
                if (s != null && s.length > finalMaxNumber) {
                    s.delete(finalMaxNumber, s.length)
                }
            }
        })
    }

    fun setLiveData(liveData: MutableLiveData<String>) {
        val isInputToChange = booleanArrayOf(false)
        // 监听 EditText 的变化
        binding.edtvInfo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 更新 LiveData
                isInputToChange[0] = true
                liveData.value = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        liveData.observeForever { value ->
            if (isInputToChange[0]) {
                isInputToChange[0] = false
                return@observeForever
            }
            binding.edtvInfo.setText(value) // 更新 EditText
            binding.edtvInfo.setSelection(value?.length ?: 0) // 光标移动到末尾
        }
    }

    private var isSetChangeValue = false
    private var isNotInputLiveData: MutableLiveData<String>? = null

    fun setNotInputLiveDataValue(value: String) {
        isNotInputLiveData?.let {
            isSetChangeValue = true
            it.value = value
        }
    }

    fun setNotInputLiveData(liveData: MutableLiveData<String>) {
        isNotInputLiveData = liveData
        isNotInputLiveData?.observeForever { value ->
            if (isSetChangeValue) {
                isSetChangeValue = false
                return@observeForever
            }
            binding.edtvInfo.setText(value) // 更新 EditText
        }
    }

    /**
     * 设置单行输入
     * @param isSingleLine true:单行输入
     */
    fun setSingleLine(isSingleLine: Boolean) {
        binding.edtvInfo.isSingleLine = isSingleLine // 设置为单行
        binding.edtvInfo.maxLines = if (isSingleLine) 1 else Int.MAX_VALUE // 最大行数
    }

}