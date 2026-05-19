//
// Created by clt on 2025/12/23.
//

#include "invoke_demo.h"
#include <string>

#include <syslog.h>

using namespace std;


extern "C" {


JNIEXPORT jint JNICALL Java_com_vectordemo_manager_KniManager_changeKotlinValue
        (JNIEnv *env, jclass jclazz, jobject jniEntity) {

    // 获取JniManager的类引用
    jclass jniManagerClass = env->FindClass("com/vectordemo/manager/KniManager");
    if (jniManagerClass == nullptr) {
        syslog(LOG_ERR,"Failed to find JniManager class");
        return -1;
    }

    // 获取changeValue方法ID
    // 方法签名需要根据参数类型生成
    // 正确签名：(Lcom/vectordemo/domain/entity/kni/KniEntity;Ljava/lang/String;IFDZBSCJ[B[I[F[D[ZLjava/util/List;)V
    // 逐段解析：
    // Lcom/vectordemo/domain/entity/kni/KniEntity; → KniEntity参数
    // Ljava/lang/String; → String参数
    // I → int
    // F → float
    // D → double
    // Z → boolean
    // B → byte
    // S → short
    // J → long
    // C → char
    // [B → byte数组
    // [I → int数组
    // [F → float数组
    // [D → double数组
    // [Z → boolean数组（你之前写成了[Z是对的，但前面的long/char顺序错了）
    // Ljava/util/List; → List<Integer>（泛型擦除后只保留List）
    jmethodID changeValueMethod = env->GetStaticMethodID(
            jniManagerClass,
            "changeValue",
            "(Lcom/vectordemo/domain/bo/kni/KniEntity;Ljava/lang/String;IFDZBSJC[B[I[F[D[ZLjava/util/List;)V"
    );

    if (changeValueMethod == nullptr) {
        syslog(LOG_ERR,"Failed to find changeValue method");
        env->DeleteLocalRef(jniManagerClass);
        return -2;
    }

    // 准备要传递的参数
    jstring newStr = env->NewStringUTF("New String from JNI");
    jint newInt = 100;
    jfloat newFloat = 100.1f;
    jdouble newDouble = 200.2;
    jboolean newBool = JNI_TRUE;
    jbyte newByte = 0x7F;
    jshort newShort = 300;
    jlong newLong = 1234567890123L;
    jchar newChar = L'Z';

    // 创建新的byte数组
    jbyteArray newByteArray = env->NewByteArray(5);
    jbyte byteData[] = {0x10, 0x20, 0x30, 0x40, 0x50};
    env->SetByteArrayRegion(newByteArray, 0, 5, byteData);

    // 创建新的int数组
    jintArray newIntArray = env->NewIntArray(5);
    jint intData[] = {10, 20, 30, 40, 50};
    env->SetIntArrayRegion(newIntArray, 0, 5, intData);

    // 创建新的float数组
    jfloatArray newFloatArray = env->NewFloatArray(5);
    jfloat floatData[] = {10.1f, 20.2f, 30.3f, 40.4f, 50.5f};
    env->SetFloatArrayRegion(newFloatArray, 0, 5, floatData);

    // 创建新的double数组
    jdoubleArray newDoubleArray = env->NewDoubleArray(5);
    jdouble doubleData[] = {10.01, 20.02, 30.03, 40.04, 50.05};
    env->SetDoubleArrayRegion(newDoubleArray, 0, 5, doubleData);

    // 创建新的boolean数组
    jbooleanArray newBoolArray = env->NewBooleanArray(5);
    jboolean boolData[] = {JNI_TRUE, JNI_FALSE, JNI_TRUE, JNI_FALSE, JNI_TRUE};
    env->SetBooleanArrayRegion(newBoolArray, 0, 5, boolData);

    // 创建新的Integer List
    jclass integerClass = env->FindClass("java/lang/Integer");
    jmethodID integerConstructor = env->GetMethodID(integerClass, "<init>", "(I)V");
    jclass listClass = env->FindClass("java/util/ArrayList");
    jmethodID listConstructor = env->GetMethodID(listClass, "<init>", "()V");
    jmethodID listAddMethod = env->GetMethodID(listClass, "add", "(Ljava/lang/Object;)Z");

    jobject newIntList = env->NewObject(listClass, listConstructor);

    // 添加元素到List
    for (int i = 1; i <= 10; i++) {
        jobject integerObj = env->NewObject(integerClass, integerConstructor, i * 100);
        env->CallBooleanMethod(newIntList, listAddMethod, integerObj);
        env->DeleteLocalRef(integerObj);
    }

    // 调用Java的changeValue方法
    env->CallStaticVoidMethod(
            jniManagerClass,
            changeValueMethod,
            jniEntity,            // JniEntity对象
            newStr,              // String
            newInt,              // int
            newFloat,            // float
            newDouble,           // double
            newBool,             // boolean
            newByte,             // byte
            newShort,            // short
            newLong,             // long
            newChar,             // char
            newByteArray,        // byte[]
            newIntArray,         // int[]
            newFloatArray,       // float[]
            newDoubleArray,      // double[]
            newBoolArray,        // boolean[]
            newIntList           // List<Integer>
    );

    // 检查是否有Java异常
    if (env->ExceptionCheck()) {
        syslog(LOG_ERR,"Exception occurred when calling changeValue");
        env->ExceptionDescribe();
        env->ExceptionClear();

        // 清理局部引用
        env->DeleteLocalRef(jniManagerClass);
        env->DeleteLocalRef(newStr);
        env->DeleteLocalRef(newByteArray);
        env->DeleteLocalRef(newIntArray);
        env->DeleteLocalRef(newFloatArray);
        env->DeleteLocalRef(newDoubleArray);
        env->DeleteLocalRef(newBoolArray);
        env->DeleteLocalRef(integerClass);
        env->DeleteLocalRef(listClass);
        env->DeleteLocalRef(newIntList);

        return -3;
    }

    syslog(LOG_DEBUG,"Successfully called changeValue method from JNI");

    // 清理局部引用
    env->DeleteLocalRef(jniManagerClass);
    env->DeleteLocalRef(newStr);
    env->DeleteLocalRef(newByteArray);
    env->DeleteLocalRef(newIntArray);
    env->DeleteLocalRef(newFloatArray);
    env->DeleteLocalRef(newDoubleArray);
    env->DeleteLocalRef(newBoolArray);
    env->DeleteLocalRef(integerClass);
    env->DeleteLocalRef(listClass);
    env->DeleteLocalRef(newIntList);

    return 0;
}

JNIEXPORT void JNICALL Java_com_vectordemo_manager_KniManager_initIntMsgCallback
        (JNIEnv* env, jobject thiz){
    // 1. 获取JVM全局引用
    JavaVM* jvm = nullptr;
    env->GetJavaVM(&jvm);
    // 2. 获取IntMsg类
    jclass msg_cls = env->FindClass("com/vectordemo/domain/bo/kni/IntMsg");
    // 3. 初始化C++的update_demo
    update_demo::get_instance().init_jni_env(jvm, thiz, msg_cls);
}


JNIEXPORT void JNICALL Java_com_vectordemo_manager_KniManager_startPushIntMsg
        (JNIEnv* env, jobject thiz, jint interval_ms) {
    update_demo::get_instance().start_push_int_msg(interval_ms);
}

JNIEXPORT void JNICALL Java_com_vectordemo_manager_KniManager_stopPushIntMsg
        (JNIEnv* env, jobject thiz) {
    update_demo::get_instance().stop_push_int_msg();
}

JNIEXPORT void JNICALL Java_com_vectordemo_manager_KniManager_throwFakeException
        (JNIEnv* env, jclass /* jclazz */) {
  jclass exClass = env->FindClass("java/lang/RuntimeException");
  if (exClass == nullptr) {
    syslog(LOG_ERR, "Failed to find RuntimeException class");
    return;
  }
  env->ThrowNew(exClass, "Fake exception thrown from C++ (KNI)");
  env->DeleteLocalRef(exClass);
}

} // extern "C"


