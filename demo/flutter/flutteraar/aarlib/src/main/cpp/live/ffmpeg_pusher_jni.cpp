#include <jni.h>
#include "ff_rtmp_pusher.h"
#include <android/log.h>

#define FF_JNI_TAG "ffmpeg_pusher_jni"
#define FFJNI_LOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO, FF_JNI_TAG, FORMAT, ##__VA_ARGS__)
#define FFJNI_LOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, FF_JNI_TAG, FORMAT, ##__VA_ARGS__)

extern "C"
JNIEXPORT jint JNICALL
Java_com_demo_aarlib_live_ffmpeg_FFmpegPushBridge_nativePushStream(
        JNIEnv *env,
        jclass clazz,
        jstring inputPath,
        jstring outputPath) {
    // JNI 入口：Java 层同步调用 FFmpeg 推流。
    int ret;
    const char *input_path = env->GetStringUTFChars(inputPath, JNI_FALSE);
    const char *output_path = env->GetStringUTFChars(outputPath, JNI_FALSE);
    FFJNI_LOGI("nativePushStream start, input=%s, output=%s", input_path, output_path);
    auto *rtmpPusher = new FFRtmpPusher();
    ret = rtmpPusher->open(input_path, output_path);
    if (ret >= 0) {
        ret = rtmpPusher->push();
    }

    rtmpPusher->close();
    delete rtmpPusher;
    env->ReleaseStringUTFChars(inputPath, input_path);
    env->ReleaseStringUTFChars(outputPath, output_path);
    if (ret < 0) {
        FFJNI_LOGE("nativePushStream failed, ret=%d", ret);
    } else {
        FFJNI_LOGI("nativePushStream success, ret=%d", ret);
    }
    return ret;
}
