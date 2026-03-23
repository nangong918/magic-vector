package com.magicvector.callback

import com.magicvector.domain.constant.VadChatState

interface OnVadChatStateChange {
    fun onChange(state: VadChatState)
}