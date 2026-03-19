package com.data.domain.fragmentActivity.aao

import androidx.lifecycle.MutableLiveData

class ChatAAo {

    val avatarUrlLd: MutableLiveData<String> = MutableLiveData("")
    val nameLd: MutableLiveData<String> = MutableLiveData("")
    val isLoadingLd: MutableLiveData<Boolean> = MutableLiveData(false)

//    var messageContactItemAo : MessageContactItemAo? = null
    val inputTextLd : MutableLiveData<String> = MutableLiveData("")
}