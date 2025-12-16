#ifndef CPPDEMO_STL_DEMO_H
#define CPPDEMO_STL_DEMO_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// 基础测试函数
JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_stringFromJNI(JNIEnv* env, jobject thiz);

// STL测试函数
JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testString(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testVector(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testMap(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testAlgorithm(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testSmartPointer(JNIEnv* env, jobject thiz);

#ifdef __cplusplus
}
#endif

#endif //CPPDEMO_STL_DEMO_H
