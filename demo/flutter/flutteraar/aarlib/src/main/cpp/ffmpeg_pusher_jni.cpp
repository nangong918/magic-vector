#include <jni.h>
#include "ff_rtmp_pusher.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_demo_aarlib_live_ffmpeg_FFmpegPushBridge_nativePushStream(
        JNIEnv *env,
        jclass clazz,
        jstring inputPath,
        jstring outputPath) {
    int ret;
    const char *input_path = env->GetStringUTFChars(inputPath, JNI_FALSE);
    const char *output_path = env->GetStringUTFChars(outputPath, JNI_FALSE);
    auto *rtmpPusher = new FFRtmpPusher();
    ret = rtmpPusher->open(input_path, output_path);
    if (ret >= 0) {
        ret = rtmpPusher->push();
    }

    rtmpPusher->close();
    delete rtmpPusher;
    env->ReleaseStringUTFChars(inputPath, input_path);
    env->ReleaseStringUTFChars(outputPath, output_path);
    return ret;
}
