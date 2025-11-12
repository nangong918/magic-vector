package com.data.domain.fragmentActivity.aao

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.atomic.AtomicReference

class AgentInfoAAo {

    val avatarAtomicUri : AtomicReference<Uri> = AtomicReference(null)
    val nameLd = MutableLiveData("")
    val descriptionLd = MutableLiveData("")
    val avatarUrlLd = MutableLiveData("")
    var agentId : String? = null

    var isSetSuccess: Boolean = false
}