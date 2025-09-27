package com.data.domain.constant.fragmentActivity.aao

import androidx.lifecycle.MutableLiveData
import com.data.domain.constant.ao.MessageContactItemAo

class ChatAAo {

    val isLoadingLd: MutableLiveData<Boolean> = MutableLiveData(false)

    var messageContactItemAo : MessageContactItemAo? = null
}