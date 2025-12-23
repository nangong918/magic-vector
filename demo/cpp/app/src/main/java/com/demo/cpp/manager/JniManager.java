package com.demo.cpp.manager;

import com.demo.cpp.domain.entity.jni.JniEntity;

import java.util.List;

public class JniManager {

    // 纯Java方法，C++调用这个

    /**
     *
     * @param jniEntity
     * @param str
     * @param intValue
     * @param floatValue
     * @param doubleValue
     * @param boolValue
     * @param byteValue
     * @param shortValue
     * @param longValue
     * @param charValue
     * @param byteArray
     * @param intArray
     * @param floatArray
     * @param doubleArray
     * @param boolArray
     * @param intList
     */
    public static void changeValue(
            JniEntity jniEntity,
            String str,
            int intValue,
            float floatValue,
            double doubleValue,
            boolean boolValue,
            byte byteValue,
            short shortValue,
            long longValue,
            char charValue,
            byte[] byteArray,
            int[] intArray,
            float[] floatArray,
            double[] doubleArray,
            boolean[] boolArray,
            List<Integer> intList
    ) {
        jniEntity.str = str;
        jniEntity.intValue = intValue;
        jniEntity.floatValue = floatValue;
        jniEntity.doubleValue = doubleValue;
        jniEntity.boolValue = boolValue;
        jniEntity.byteValue = byteValue;
        jniEntity.shortValue = shortValue;
        jniEntity.longValue = longValue;
        jniEntity.charValue = charValue;
        jniEntity.byteArray = byteArray;
        jniEntity.intArray = intArray;
        jniEntity.floatArray = floatArray;
        jniEntity.doubleArray = doubleArray;
        jniEntity.boolArray = boolArray;
        jniEntity.intList = intList;
    }

    /// java-native方法定义
    // 在jni中调用JniManager.changeValue方法
    native public static int changeJavaValue(JniEntity jniEntity);

}
