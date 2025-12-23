//
// Created by clt on 2025/12/23.
//

#ifndef CPPDEMO_INVOKE_DEMO_H
#define CPPDEMO_INVOKE_DEMO_H

#include <jni.h>
#include "update/update_demo.h"


#ifdef __cplusplus
extern "C" {
#endif

/// JNI 测试 （Java调用Cpp写需要sync，调用Cpp读不需要）
// Cpp 调用 Java对象，Java函数，给Java对象赋值
/**
 * 在JNI中调用Java的changeValue方法
 * @param env           JNI环境指针
 * @param jclazz        Java对象实例
 * @param jniEntity     Java的JniEntity对象
 * @return 执行结果，0表示成功，其他表示失败
 */
JNIEXPORT jint JNICALL Java_com_demo_cpp_manager_JniManager_changeJavaValue
        (JNIEnv *env, jclass jclazz, jobject jniEntity);

/**
 * JNI初始化：绑定Java回调环境
 * @param env
 * @param thiz
 * @return
 */
JNIEXPORT void JNICALL Java_com_demo_cpp_manager_JniManager_initIntMsgCallback
        (JNIEnv* env, jobject thiz);

/**
 * 启动推送
 * @param env
 * @param thiz
 * @param interval_ms
 * @return
 */
JNIEXPORT void JNICALL Java_com_demo_cpp_manager_JniManager_startPushIntMsg
        (JNIEnv* env, jobject thiz, jint interval_ms);

/**
 * 停止推送
 * @param env
 * @param thiz
 * @return
 */
JNIEXPORT void JNICALL Java_com_demo_cpp_manager_JniManager_stopPushIntMsg
        (JNIEnv* env, jobject thiz);

#ifdef __cplusplus
}
#endif


#endif //CPPDEMO_INVOKE_DEMO_H
