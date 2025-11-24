package com.magicvector.utils.chat

import com.google.gson.Gson

class RealtimeChatWsClient(
    private val gson : Gson,
    private val baseUrl: String
) : AbstractWsClient(gson = gson, baseUrl = baseUrl){


}