package com.vectordemo.domain.entity.kni

class KniEntity {
    @JvmField
    var str: String = "hello world"

    @JvmField
    var intValue: Int = 1

    @JvmField
    var floatValue: Float = 1.1f

    @JvmField
    var doubleValue: Double = 2.2

    @JvmField
    var boolValue: Boolean = true

    @JvmField
    var byteValue: Byte = 0x0f

    @JvmField
    var shortValue: Short = 121

    @JvmField
    var longValue: Long = System.currentTimeMillis()

    @JvmField
    var charValue: Char = 'a'

    @JvmField
    var byteArray: ByteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)

    @JvmField
    var intArray: IntArray = intArrayOf(1, 2, 3, 4, 5)

    @JvmField
    var floatArray: FloatArray = floatArrayOf(1.1f, 2.2f, 3.3f, 4.4f, 5.5f)

    @JvmField
    var doubleArray: DoubleArray = doubleArrayOf(1.1, 2.2, 3.3, 4.4, 5.5)

    @JvmField
    var boolArray: BooleanArray = booleanArrayOf(true, false, true, false, true)

    @JvmField
    var intList: List<Int> = listOf(1, 2, 3, 4, 5)
}
