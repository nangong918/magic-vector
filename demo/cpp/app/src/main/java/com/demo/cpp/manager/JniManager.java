package com.demo.cpp.manager;

import android.util.Log;

import com.demo.cpp.domain.entity.jni.IntMsg;
import com.demo.cpp.domain.entity.jni.JniEntity;

import java.util.List;

public class JniManager {

    public static final String TAG = "JniManager";

    static {
        System.loadLibrary("cpp"); // 加载JNI库（名称与CMakeLists一致）
    }

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

    // C++回调的Java方法（必须与C++中GetMethodID的方法名/签名一致）
    private void onIntMsgReceived(IntMsg msg) {
        // 接收C++推送的int++消息
        Log.i(TAG, "收到C++推送的int消息：" + msg.value);
    }

    /// java-native方法定义
    // 在jni中调用JniManager.changeValue方法
    public native static int changeJavaValue(JniEntity jniEntity);

    // 初始化JNI回调环境
    public static native void initIntMsgCallback();

    // 启动推送
    public static native void startPushIntMsg(int interval_ms);

    // 停止推送
    public static native void stopPushIntMsg();
}
