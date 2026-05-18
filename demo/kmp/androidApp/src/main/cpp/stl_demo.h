#ifndef VECTORDEMO_STL_DEMO_H
#define VECTORDEMO_STL_DEMO_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testString(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testVector(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testMap(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testAlgorithm(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testSmartPointer(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testList(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testSet(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testDeque(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testStack(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testQueue(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_vectordemo_activity_STLActivity_testPriorityQueue(JNIEnv* env, jobject thiz);

#ifdef __cplusplus
}
#endif

#endif //VECTORDEMO_STL_DEMO_H
