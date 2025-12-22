package com.demo.cpp.manager

import com.demo.cpp.domain.entity.jni.JniEntity

object JniManager {

    /// java 方法定义
    // Cpp 调用 Java对象，Java函数，给Java对象赋值
    fun changeValue(
        jniEntity: JniEntity,
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
        intList: MutableList<Int>
    ){
        jniEntity.str = str
        jniEntity.intValue = intValue
        jniEntity.floatValue = floatValue
        jniEntity.doubleValue = doubleValue
        jniEntity.boolValue = boolValue
        jniEntity.byteValue = byteValue
        jniEntity.shortValue = shortValue
        jniEntity.longValue = longValue
        jniEntity.charValue = charValue
        jniEntity.byteArray = byteArray
        jniEntity.intArray = intArray
        jniEntity.floatArray = floatArray
        jniEntity.doubleArray = doubleArray
        jniEntity.boolArray = boolArray
        jniEntity.intList = intList
    }

    /// java-native方法定义
    // 在jni中调用JniManager.changeValue方法
    external fun changeJavaValue(jniEntity: JniEntity): Int
}