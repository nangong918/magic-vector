package com.magicvector.callback

import com.data.domain.constant.VadChatState

interface OnVadChatStateChange {
    fun onChange(state: VadChatState)
}