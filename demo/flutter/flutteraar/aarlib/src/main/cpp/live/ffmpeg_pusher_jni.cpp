/**
 * @file ffmpeg_pusher_jni.cpp
 * @brief JNI：`FFmpegPushBridge.nativePushStream` → `FFRtmpPusher`（文件/网络输入转 RTMP 或 RTSP）。
 *
 * Java 侧一般用单线程 Executor 调用本 native，避免阻塞 UI。
 * 返回值为 FFmpeg 错误码：>=0 通常表示跑完 push；<0 见 libav error。
 */

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
    (void) clazz;
    int ret;
    const char *input_path = env->GetStringUTFChars(inputPath, JNI_FALSE);   // 将 Java String 转为 UTF-8 C 字符串
    const char *output_path = env->GetStringUTFChars(outputPath, JNI_FALSE); // 输出 URL（rtmp/rtsp 等）
    FFJNI_LOGI("nativePushStream start, input=%s, output=%s", input_path, output_path);

    auto *rtmpPusher = new FFRtmpPusher();
    ret = rtmpPusher->open(input_path, output_path); // 打开输入、创建输出、写 header
    if (ret >= 0) {
        ret = rtmpPusher->push();                    // 循环读包、修正时间戳、写入输出
    }

    rtmpPusher->close();                             // 写 trailer、释放 AVFormatContext
    delete rtmpPusher;

    env->ReleaseStringUTFChars(inputPath, input_path);   // 配对释放 JNI 字符串锁
    env->ReleaseStringUTFChars(outputPath, output_path);

    if (ret < 0) {
        FFJNI_LOGE("nativePushStream failed, ret=%d", ret);
    } else {
        FFJNI_LOGI("nativePushStream success, ret=%d", ret);
    }
    return ret; // 返回给 Java：0 或正数多为成功路径；负数 libav 错误码
}
