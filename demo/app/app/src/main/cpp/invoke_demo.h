//
// Created by clt on 2025/12/23.
//

#ifndef VECTORDEMO_INVOKE_DEMO_H
#define VECTORDEMO_INVOKE_DEMO_H

#include <jni.h>
#include "update/update_demo.h"

#ifdef __cplusplus
extern "C" {
#endif

/// KNI 测试 （Kotlin 调用 Cpp 写需要 sync，调用 Cpp 读不需要）
// Cpp 调用 Kotlin 对象、Kotlin 函数，给 Kotlin 对象赋值
/**
 * 在 KNI 中调用 Kotlin 的 changeValue 方法
 * @param env           JNI 环境指针
 * @param jclazz        Kotlin 类
 * @param kniEntity     Kotlin 的 KniEntity 对象
 * @return 执行结果，0 表示成功，其他表示失败
 */
JNIEXPORT jint JNICALL Java_com_vectordemo_manager_KniManager_changeKotlinValue
        (JNIEnv *env, jclass jclazz, jobject kniEntity);

/**
 * C++ 主动抛出假异常给 Kotlin
 */
JNIEXPORT void JNICALL Java_com_vectordemo_manager_KniManager_throwFakeException
        (JNIEnv *env, jclass jclazz);

/**
 * KNI 初始化：绑定 Kotlin 回调环境
 */
JNIEXPORT void JNICALL Java_com_vectordemo_manager_KniManager_initIntMsgCallback
        (JNIEnv* env, jobject thiz);

/**
 * 启动推送
 */
JNIEXPORT void JNICALL Java_com_vectordemo_manager_KniManager_startPushIntMsg
        (JNIEnv* env, jobject thiz, jint interval_ms);

/**
 * 停止推送
 */
JNIEXPORT void JNICALL Java_com_vectordemo_manager_KniManager_stopPushIntMsg
        (JNIEnv* env, jobject thiz);

#ifdef __cplusplus
}
#endif

#endif //VECTORDEMO_INVOKE_DEMO_H
