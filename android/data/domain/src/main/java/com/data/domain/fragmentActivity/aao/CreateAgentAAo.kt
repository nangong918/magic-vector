package com.data.domain.fragmentActivity.aao

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.atomic.AtomicReference

class CreateAgentAAo {

    val avatarAtomicUrl : AtomicReference<Uri> = AtomicReference(null)
    val nameLd = MutableLiveData("")
    val descriptionLd = MutableLiveData("")

}