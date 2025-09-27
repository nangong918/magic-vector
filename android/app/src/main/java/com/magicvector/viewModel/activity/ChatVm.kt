package com.magicvector.viewModel.activity

import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.ViewModel
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.fragmentActivity.aao.ChatAAo


class ChatVm(

) : ViewModel(){

    companion object {
        val TAG: String = ChatVm::class.java.name
    }

    //---------------------------AAo Ld---------------------------

    val aao = ChatAAo()

    fun initAAo(messageContactItemAo : MessageContactItemAo?){
        aao.messageContactItemAo = messageContactItemAo
    }

    //---------------------------NetWork---------------------------

    //---------------------------Logic---------------------------

    fun getTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    aao.inputTextLd.value = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

}