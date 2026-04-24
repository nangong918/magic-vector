#include <jni.h>
#include <string>
#include "PacketQueue.h"
#include "PushInterface.h"
#include "VideoStream.h"
#include "AudioStream.h"

/**
 * Java_com_demo_aarlib_live_LivePusherBridge_* JNI 函数映射宏。
 */
#define LIVE_PUSHER_FUNC(RETURN_TYPE, FUNC_NAME, ...) \
    extern "C" \
    JNIEXPORT RETURN_TYPE JNICALL Java_com_demo_aarlib_live_LivePusherBridge_ ## FUNC_NAME \
    (JNIEnv *env, jobject instance, ##__VA_ARGS__)

PacketQueue<RTMPPacket *> packets;
VideoStream *videoStream = nullptr;
AudioStream *audioStream = nullptr;

std::atomic<bool> isPushing;
uint32_t start_time;

JavaVM *javaVM;
jobject jobject_error;

/**
 * JNI 加载入口：缓存 JavaVM。
 */
jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    return JNI_VERSION_1_6;
}

/**
 * 把 native 错误回调到 Java 层 errorFromNative(int)。
 */
void throwErrToJava(int error_code) {
    JNIEnv *env;
    javaVM->AttachCurrentThread(&env, nullptr);
    jclass classErr = env->GetObjectClass(jobject_error);
    jmethodID methodErr = env->GetMethodID(classErr, "errorFromNative", "(I)V");
    env->CallVoidMethod(jobject_error, methodErr, error_code);
    javaVM->DetachCurrentThread();
}

/**
 * 编码输出回调：给 RTMP 包打时间戳并入队。
 */
void callback(RTMPPacket *packet) {
    if (packet) {
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        packets.push(packet);
    }
}

/**
 * RTMPPacket 资源释放函数。
 */
void releasePackets(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = nullptr;
    }
}

/**
 * 推流发送线程：
 * - 建立 RTMP 连接；
 * - 从队列取包并发送；
 * - 失败时上报错误并收尾。
 */
void *start(void *args) {
    char *url = static_cast<char *>(args);
    RTMP *rtmp = nullptr;
    do {
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("RTMP_Alloc fail");
            break;
        }
        RTMP_Init(rtmp);
        int ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("RTMP_SetupURL:%s", url);
            break;
        }
        rtmp->Link.timeout = 5;
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, nullptr);
        if (!ret) {
            LOGE("RTMP_Connect:%s", url);
            throwErrToJava(ERROR_RTMP_CONNECT);
            break;
        }
        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            LOGE("RTMP_ConnectStream:%s", url);
            throwErrToJava(ERROR_RTMP_CONNECT_STREAM);
            break;
        }
        start_time = RTMP_GetTime();
        LOGI("RTMP connected, start pushing...");
        isPushing = true;
        packets.setRunning(true);
        callback(audioStream->getAudioTag());
        RTMPPacket *packet = nullptr;
        while (isPushing) {
            packets.pop(packet);
            if (!isPushing) {
                break;
            }
            if (!packet) {
                continue;
            }

            packet->m_nInfoField2 = rtmp->m_stream_id;
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("RTMP_SendPacket fail...");
                throwErrToJava(ERROR_RTMP_SEND_PACKET);
                break;
            }
        }
        releasePackets(packet);
    } while (0);
    isPushing = false;
    packets.setRunning(false);
    packets.clear();
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete (url);
    return nullptr;
}

LIVE_PUSHER_FUNC(void, native_1init) {
    // 初始化音视频编码对象与回调。
    LOGI("native init...");
    videoStream = new VideoStream();
    videoStream->setVideoCallback(callback);
    audioStream = new AudioStream();
    audioStream->setAudioCallback(callback);
    packets.setReleaseCallback(releasePackets);
    jobject_error = env->NewGlobalRef(instance);
}

LIVE_PUSHER_FUNC(void, native_1setVideoCodecInfo,
        jint width, jint height, jint fps, jint bitrate) {
    LOGI("native_setVideoCodecInfo, width=%d, height=%d, fps=%d, bitrate=%d", width, height, fps, bitrate);
    if (videoStream) {
        int ret = videoStream->setVideoEncInfo(width, height, fps, bitrate);
        if (ret < 0) {
            throwErrToJava(ERROR_VIDEO_ENCODER_OPEN);
        }
    }
}

LIVE_PUSHER_FUNC(void, native_1start, jstring path_) {
    LOGI("native start...");
    if (isPushing) {
        return;
    }
    const char *path = env->GetStringUTFChars(path_, nullptr);
    char *url = new char[strlen(path) + 1];
    strcpy(url, path);

    std::thread pushThread(start, url);
    pushThread.detach();
    env->ReleaseStringUTFChars(path_, path);
}

LIVE_PUSHER_FUNC(void, native_1pushVideo, jbyteArray yuv, jint frame_type) {
    if (!videoStream || !isPushing) {
        return;
    }
    jbyte *yuv_plane = env->GetByteArrayElements(yuv, JNI_FALSE);
    videoStream->encodeVideo(yuv_plane, frame_type);
    env->ReleaseByteArrayElements(yuv, yuv_plane, 0);
}

LIVE_PUSHER_FUNC(void, native_1setAudioCodecInfo, jint sampleRateInHz, jint channels) {
    LOGI("native_setAudioCodecInfo, sampleRate=%d, channels=%d", sampleRateInHz, channels);
    if (audioStream) {
        int ret = audioStream->setAudioEncInfo(sampleRateInHz, channels);
        if (ret < 0) {
            throwErrToJava(ERROR_AUDIO_ENCODER_OPEN);
        }
    }
}

LIVE_PUSHER_FUNC(jint, native_1getInputSamples) {
    if (audioStream) {
        return audioStream->getInputSamples();
    }
    return -1;
}

LIVE_PUSHER_FUNC(void, native_1pushAudio, jbyteArray data_) {
    if (!audioStream || !isPushing) {
        return;
    }
    jbyte *data = env->GetByteArrayElements(data_, nullptr);
    audioStream->encodeData(data);
    env->ReleaseByteArrayElements(data_, data, 0);
}

LIVE_PUSHER_FUNC(void, native_1stop) {
    LOGI("native stop...");
    isPushing = false;
    packets.setRunning(false);
}

LIVE_PUSHER_FUNC(void, native_1release) {
    LOGI("native release...");
    env->DeleteGlobalRef(jobject_error);
    delete videoStream;
    videoStream = nullptr;
    delete audioStream;
    audioStream = nullptr;
}
