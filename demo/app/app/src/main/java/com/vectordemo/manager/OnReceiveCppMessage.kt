package com.vectordemo.manager

import com.vectordemo.domain.bo.kni.IntMsg

fun interface OnReceiveCppMessage {
    fun onReceiveCppMessage(msg: IntMsg)
}
