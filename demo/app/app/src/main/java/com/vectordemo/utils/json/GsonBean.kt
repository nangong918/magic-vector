package com.vectordemo.utils.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

interface GsonBean {
    companion object {
        val GSON: Gson by lazy { GsonBuilder().setPrettyPrinting().create() }
    }

    fun toJsonString(): String = GSON.toJson(toJson())
    fun toJson(): JsonObject = GSON.toJsonTree(this).asJsonObject
}
