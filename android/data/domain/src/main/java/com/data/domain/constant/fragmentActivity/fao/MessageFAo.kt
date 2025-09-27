package com.data.domain.constant.fragmentActivity.fao

import androidx.lifecycle.MutableLiveData
import com.data.domain.constant.ao.MessageContactItemAo

class MessageFAo {

    val messageContactList: MutableList<MessageContactItemAo> = mutableListOf()
    val messageContactCountLd: MutableLiveData<Int> = MutableLiveData(0)

}