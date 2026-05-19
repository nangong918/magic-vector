package com.vectordemo.manager

import com.vectordemo.domain.bo.kni.IntMsg
import com.vectordemo.domain.bo.kni.KniEntity

class KniManager private constructor() {

    /**
     * C++ 回调的 Kotlin 方法（必须与 C++ 中 GetMethodID 的方法名/签名一致）
     * @param msg   IntMsg对象
     */
    fun onIntMsgReceived(msg: IntMsg?) {
        if (onReceiveCppMessage != null && msg != null) {
            onReceiveCppMessage!!.onReceiveCppMessage(msg)
        }
    }

    /**
     * 初始化 KNI 回调环境
     */
    external fun initIntMsgCallback()

    /**
     * 启动推送
     * @param intervalMs 推送间隔
     */
    external fun startPushIntMsg(intervalMs: Int)

    /**
     * 停止推送
     */
    external fun stopPushIntMsg()

    companion object {
        const val TAG = "KniManager"

        @JvmStatic
        var onReceiveCppMessage: OnReceiveCppMessage? = null

        @JvmStatic
        val instance: KniManager = KniManager()

        init {
            System.loadLibrary("vectordemo")
        }

        /**
         * 纯 Kotlin 方法，C++ 调用这个
         * @param kniEntity     KniEntity对象     com/vectordemo/domain/entity/kni/KniEntity
         * @param str           字符串             Ljava/lang/String
         * @param intValue      整数              I
         * @param floatValue    浮点数f            F
         * @param doubleValue   浮点数d            D
         * @param boolValue     布尔值             Z
         * @param byteValue     字节              B
         * @param shortValue    短整型             S
         * @param longValue     长整型             J
         * @param charValue     字符              C
         * @param byteArray     字节数组            [B
         * @param intArray      整型数组            [I
         * @param floatArray    浮点数数组           [F
         * @param doubleArray   浮点数数组           [D
         * @param boolArray     布尔数组            [Z
         * @param intList       整型列表            Ljava/util/List （泛型擦除）
         */
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
            intList: List<Int>
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

        /**
         * 在 KNI 中调用 KniManager.changeValue 方法
         * @param kniEntity KniEntity对象
         * @return  是否成功
         */
        @JvmStatic
        external fun changeKotlinValue(kniEntity: KniEntity): Int

        /**
         * C++ 主动抛出假异常（RuntimeException），由 Kotlin 侧 try/catch 接收
         */
        @JvmStatic
        external fun throwFakeException()
    }
}
