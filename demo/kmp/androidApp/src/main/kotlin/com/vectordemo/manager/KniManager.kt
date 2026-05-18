package com.vectordemo.manager

import com.vectordemo.domain.entity.kni.IntMsg
import com.vectordemo.domain.entity.kni.KniEntity

class KniManager private constructor() {
    fun onIntMsgReceived(msg: IntMsg?) {
        val callback = onReceiveCppMessage
        if (callback != null && msg != null) {
            callback.onReceiveCppMessage(msg)
        }
    }

    external fun initIntMsgCallback()
    external fun startPushIntMsg(intervalMs: Int)
    external fun stopPushIntMsg()

    companion object {
        @JvmStatic
        var onReceiveCppMessage: OnReceiveCppMessage? = null

        @JvmStatic
        val instance: KniManager = KniManager()

        init {
            System.loadLibrary("vectordemo")
        }

        @JvmStatic
        fun changeValue(
            kniEntity: KniEntity,
            str: String,
            intValue: Int,
            floatValue: Float,
            doubleValue: Double,
            boolValue: Boolean,
            byteValue: Byte,
            shortValue: Short,
            longValue: Long,
            charValue: Char,
            byteArray: ByteArray,
            intArray: IntArray,
            floatArray: FloatArray,
            doubleArray: DoubleArray,
            boolArray: BooleanArray,
            intList: List<Int>,
        ) {
            kniEntity.str = str
            kniEntity.intValue = intValue
            kniEntity.floatValue = floatValue
            kniEntity.doubleValue = doubleValue
            kniEntity.boolValue = boolValue
            kniEntity.byteValue = byteValue
            kniEntity.shortValue = shortValue
            kniEntity.longValue = longValue
            kniEntity.charValue = charValue
            kniEntity.byteArray = byteArray
            kniEntity.intArray = intArray
            kniEntity.floatArray = floatArray
            kniEntity.doubleArray = doubleArray
            kniEntity.boolArray = boolArray
            kniEntity.intList = intList
        }

        @JvmStatic
        external fun changeKotlinValue(kniEntity: KniEntity): Int

        @JvmStatic
        external fun throwFakeException()
    }
}
