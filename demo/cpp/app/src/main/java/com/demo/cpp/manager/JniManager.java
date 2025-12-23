package com.demo.cpp.manager;


import com.demo.cpp.domain.entity.jni.IntMsg;
import com.demo.cpp.domain.entity.jni.JniEntity;

import java.util.List;

public class JniManager {

    public static final String TAG = "JniManager";

    private static final JniManager instance = new JniManager();
    public static OnReceiveCppMessage onReceiveCppMessage;

    public static JniManager getInstance() {
        return instance;
    }

    static {
        System.loadLibrary("cpp"); // 加载JNI库（名称与CMakeLists一致）
    }

    // 纯Java方法，C++调用这个

    /**
     * 纯Java方法，C++调用这个
     * @param jniEntity     JniEntity对象     com/demo/cpp/domain/entity/jni/JniEntity
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

    /**
     * C++回调的Java方法（必须与C++中GetMethodID的方法名/签名一致）
     * @param msg   IntMsg对象
     */
    public void onIntMsgReceived(IntMsg msg) {
        if (onReceiveCppMessage != null && msg != null){
            onReceiveCppMessage.onReceiveCppMessage(msg);
        }
    }

    /// java-native方法定义
    /**
     * 在jni中调用JniManager.changeValue方法
     * @param jniEntity JniEntity对象
     * @return  是否成功
     */
    public native static int changeJavaValue(JniEntity jniEntity);

    /**
     * 初始化JNI回调环境
     */
    public native void initIntMsgCallback();

    /**
     * 启动推送
     * @param interval_ms   推送间隔
     */
    public native void startPushIntMsg(int interval_ms);

    /**
     * 停止推送
     */
    public native void stopPushIntMsg();
}
