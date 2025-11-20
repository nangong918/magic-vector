#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_magicvector_demo_StartActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello From JNI C++";
    return env->NewStringUTF(hello.c_str());
}