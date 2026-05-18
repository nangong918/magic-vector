package com.vectordemo.manager

import com.vectordemo.domain.entity.kni.IntMsg

fun interface OnReceiveCppMessage {
    fun onReceiveCppMessage(msg: IntMsg)
}
