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

// 新增数据结构测试
JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testList(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testSet(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testDeque(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testStack(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testQueue(JNIEnv* env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_com_demo_cpp_STLActivity_testPriorityQueue(JNIEnv* env, jobject thiz);

/// JNI 测试 （Java调用Cpp写需要sync，调用Cpp读不需要）
// Cpp 调用 Java对象，Java函数，给Java对象赋值
/**
 * 在JNI中调用Java的changeValue方法
 * @param env JNI环境指针
 * @param jobj Java对象实例
 * @param jniEntity Java的JniEntity对象
 * @return 执行结果，0表示成功，其他表示失败
 */
JNIEXPORT jint JNICALL Java_com_demo_cpp_manager_JniManager_changeJavaValue
        (JNIEnv *env, jobject jobj, jobject jniEntity);

#ifdef __cplusplus
}
#endif

#endif //CPPDEMO_STL_DEMO_H
