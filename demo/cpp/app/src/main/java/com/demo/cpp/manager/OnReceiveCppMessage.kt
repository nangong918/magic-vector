package com.demo.cpp.manager

import com.demo.cpp.domain.entity.jni.IntMsg

interface OnReceiveCppMessage {
    fun onReceiveCppMessage(msg: IntMsg)
}