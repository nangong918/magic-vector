/**
 * @file RtmpPusher.cpp
 * @brief JNI：Java `com.demo.aarlib.live.LivePusherBridge` ↔ Native 实时 RTMP 推流。
 *
 * 【架构】
 * - VideoStream / AudioStream：各自产出 RTMPPacket，经同一 callback() 打时间戳后入 PacketQueue。
 * - start(url) 内新建线程：RTMP_Connect → 先发 AAC 序列头 → while pop 发送。
 * - Java 侧仅在 isPushing==true 时 native_push*，避免未连接就编码堆积。
 */

#include <jni.h>
#include <cstring>
#include <string>
#include <atomic>
#include <mutex>
#include <thread>
#include "PacketQueue.h"
#include "PushInterface.h"
#include "VideoStream.h"
#include "AudioStream.h"

/**
 * Java_com_demo_aarlib_live_LivePusherBridge_* JNI 函数映射宏。
 * 将 RETURN_TYPE、FUNC_NAME 展开为完整 JNI 符号名（Java_包名_类名_方法名）。
 */
#define LIVE_PUSHER_FUNC(RETURN_TYPE, FUNC_NAME, ...) \
    extern "C" \
    JNIEXPORT RETURN_TYPE JNICALL Java_com_demo_aarlib_live_LivePusherBridge_ ## FUNC_NAME \
    (JNIEnv *env, jobject instance, ##__VA_ARGS__)

PacketQueue<RTMPPacket *> packets;
VideoStream *videoStream = nullptr;
AudioStream *audioStream = nullptr;

/** 推流线程已成功 RTMP_ConnectStream：允许 native_push* 把音视频数据编码并进队发送 */
std::atomic<bool> isPushing;
/** native_start 已发起连接或线程尚未结束：防止重复 start 造成多线程、重复连接 */
std::atomic<bool> isStarting;
/** 用户调用 native_stop / native_release：连接失败或中途断开时不应向 Java 弹错误（视为主动停止） */
std::atomic<bool> stopRequested;
/** 推流会话起点对应的 RTMP_GetTime()：callback 里用「当前时间 − start_time」生成 FLV/RTMP 相对毫秒时间戳 */
uint32_t start_time;

JavaVM *javaVM;
/** Java 层 LivePusherBridge 实例的全局引用；throwErrToJava 通过它反射调用 errorFromNative(int) */
jobject jobject_error;
/** 执行 start() 的工作线程：负责 RTMP 连接、从队列取包、RTMP_SendPacket */
std::thread pushThread;
/** 保护 jobject_error 的创建、读取、清空：与 throwErrToJava（可能来自任意线程）及 JNI 函数交错防竞态 */
std::mutex callbackMutex;
/** 保护 pushThread 的创建与「收尾时 detach」判断，避免与 native_start / 线程退出并发改同一 thread 对象 */
std::mutex pushThreadMutex;

/**
 * JNI 加载入口：缓存 JavaVM。
 * So 库载入时调用一次；保存 JavaVM 供子线程 AttachCurrentThread。
 */
jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void) reserved;
    javaVM = vm;
    return JNI_VERSION_1_6;
}

/**
 * 把 native 错误码投递到 Java 层 LivePusherBridge.errorFromNative(int)。
 * 任意线程可调用（编码线程、推流线程）；内部 AttachCurrentThread 取得 JNIEnv，用完 Detach。
 */
void throwErrToJava(int error_code) {
    // lock_guard：构造时 lock(callbackMutex)，函数返回析构时 unlock；防止与 native_1init/release 同时碰 jobject_error。
    std::lock_guard<std::mutex> lock(callbackMutex);
    if (jobject_error == nullptr) {
        LOGE("throwErrToJava skipped, callback object is null, code=%d", error_code);
        return;
    }
    JNIEnv *env;
    // 非 JNI 附加线程没有 JNIEnv，必须从 JavaVM 绑定到当前 pthread。
    if (javaVM->AttachCurrentThread(&env, nullptr) != JNI_OK) {
        LOGE("AttachCurrentThread failed, code=%d", error_code);
        return;
    }
    // 取 Java 对象类与方法 ID，约定签名为 void errorFromNative(int)。
    jclass classErr = env->GetObjectClass(jobject_error);
    jmethodID methodErr = env->GetMethodID(classErr, "errorFromNative", "(I)V");
    if (methodErr != nullptr) {
        env->CallVoidMethod(jobject_error, methodErr, error_code);
    } else {
        LOGE("errorFromNative method not found");
    }
    // 与本线程解绑，避免 JVM 维护过多附加线程。
    javaVM->DetachCurrentThread();
}

/**
 * 编码输出回调：给 RTMP 包打时间戳并入队。
 * start_time 在 RTMP 握手成功后设为当前 RTMP_GetTime()，保证首包时间从 0 附近递增。
 */
void callback(RTMPPacket *packet) {
    if (packet) {
        // RTMP 时间戳常用相对毫秒：与连接时刻对齐，播放器才能连续解码。
        packet->m_nTimeStamp = RTMP_GetTime() - start_time;
        // push 仅在队列 running 时收包；发送线程 pop 后 RTMP_SendPacket。
        packets.push(packet);
    }
}

/**
 * RTMPPacket 资源释放函数。
 * librtmp 分配的 packet 需 RTMPPacket_Free；外层 new 的 wrapper 需 delete。
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
 *
 * 参数为 new[] 出来的 RTMP URL 字符串，线程结束时 delete。
 * 步骤：Alloc→SetupURL→EnableWrite→Connect→ConnectStream→记 start_time→
 * setRunning(true)→callback 音频序列头→循环 pop 并 RTMP_SendPacket。
 * 停止：isPushing=false 或发送失败；clear 队列释放残留 packet。
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
        // 套接字阻塞超时（秒），避免坏地址无限挂起。
        rtmp->Link.timeout = 5;
        // 推流端必须开启写模式（发布），否则按播放连接处理。
        RTMP_EnableWrite(rtmp);
        ret = RTMP_Connect(rtmp, nullptr);
        if (!ret) {
            LOGE("RTMP_Connect:%s", url);
            if (!stopRequested) {
                throwErrToJava(ERROR_RTMP_CONNECT);
            }
            break;
        }
        // 在已建立的 RTMP 连接上选中要发布的流（chunk stream / stream id）。
        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            LOGE("RTMP_ConnectStream:%s", url);
            if (!stopRequested) {
                throwErrToJava(ERROR_RTMP_CONNECT_STREAM);
            }
            break;
        }
        // start 与 stop 竞态：若在连接过程中用户已 stop，不再进入发送循环。
        if (stopRequested) {
            LOGI("start thread exit early due to stopRequested");
            break;
        }
        // 作为相对时间戳基准；此后编码线程产生的包时间戳均相对该时刻。
        start_time = RTMP_GetTime();
        LOGI("RTMP connected, start pushing...");
        // 允许 Java 侧 native_push*；PacketQueue 开始接受 push。
        isPushing = true;
        packets.setRunning(true);
        // FLV 音频必须先发 AAC sequence header（AudioSpecificConfig），解码器才能解后续 AAC raw。
        callback(audioStream->getAudioTag());
        RTMPPacket *packet = nullptr;
        // pop 非阻塞：队列为空时返回 0，循环继续直到 isPushing 变 false（忙等模型）。
        while (isPushing) {
            packets.pop(packet);
            if (!isPushing) {
                break;
            }
            if (!packet) {
                continue;
            }

            // librtmp：把 RTMP 包关联到当前 publish 的流 id（握手后由服务端分配）。
            packet->m_nInfoField2 = rtmp->m_stream_id;
            // 第三个参数 1 表示使用 chunk 缓存队列发送（librtmp 常规写法）。
            ret = RTMP_SendPacket(rtmp, packet, 1);
            releasePackets(packet);
            if (!ret) {
                LOGE("RTMP_SendPacket fail...");
                if (!stopRequested) {
                    throwErrToJava(ERROR_RTMP_SEND_PACKET);
                }
                break;
            }
        }
        releasePackets(packet);
    } while (0);
    isPushing = false;
    // 拒绝新包并入队；发送线程即将清空残留。
    packets.setRunning(false);
    packets.clear();
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete (url);
    isStarting = false;
    {
        // 仅推流线程自身结束时 detach：避免 join 阻塞 UI；与其它线程改 pushThread 的操作串行。
        std::lock_guard<std::mutex> threadLock(pushThreadMutex);
        if (pushThread.joinable() && pushThread.get_id() == std::this_thread::get_id()) {
            pushThread.detach();
        }
    }
    return nullptr;
}

/**
 * native_1init：Java 构造函数里调用。
 * 初始化音视频编码对象与回调；保存 LivePusherBridge 全局引用供报错。
 */
LIVE_PUSHER_FUNC(void, native_1init) {
    // 初始化音视频编码对象与回调。
    LOGI("native init...");
    videoStream = new VideoStream();
    videoStream->setVideoCallback(callback);
    audioStream = new AudioStream();
    audioStream->setAudioCallback(callback);
    // 队列满/停止时 clear 会调 releasePackets，释放 RTMPPacket 堆内存。
    packets.setReleaseCallback(releasePackets);
    isPushing = false;
    isStarting = false;
    stopRequested = false;
    // std::lock_guard<std::mutex> lock(callbackMutex)：
    //   - lock_guard 在构造时自动 lock(mutex)，析构（离开作用域）时自动 unlock，异常安全。
    //   - 此处与 throwErrToJava()、native_1release() 共用 callbackMutex：保证「创建/替换 jobject_error」与
    //     「另一线程回调 Java」或「release 里 DeleteGlobalRef」不会交叉执行，避免悬空 jobject 或 JNI 崩溃。
    std::lock_guard<std::mutex> lock(callbackMutex);
    // GlobalRef：跨越 JNI 帧长期持有 Java 对象；必须在 release 里配对 DeleteGlobalRef。
    jobject_error = env->NewGlobalRef(instance);
}

/** 对应 Java updateVideoCodecInfo / 初始化宽高帧率码率 */
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

/**
 * native_1start：启动推流线程。
 * 若已有 joinable 线程则拒绝重复启动（防止泄漏）；url 复制到堆上交给 std::thread，线程内 delete。
 */
LIVE_PUSHER_FUNC(void, native_1start, jstring path_) {
    LOGI("native start...");
    // 已在推或正在连接：忽略重复 start，避免多条 TCP、多个 worker。
    if (isPushing || isStarting) {
        LOGI("native start ignored, isPushing=%d, isStarting=%d", (int)isPushing.load(), (int)isStarting.load());
        return;
    }
    stopRequested = false;
    isStarting = true;
    const char *path = env->GetStringUTFChars(path_, nullptr);
    char *url = new char[strlen(path) + 1];
    strcpy(url, path);

    {
        std::lock_guard<std::mutex> threadLock(pushThreadMutex);
        // joinable 表示已有线程对象关联到底层线程且尚未 join/detach：此处拒绝再起一线程防止泄漏。
        if (pushThread.joinable()) {
            LOGI("native start: previous pushThread joinable, skip restart");
            delete[] url;
            env->ReleaseStringUTFChars(path_, path);
            isStarting = false;
            return;
        }
        // start 在工作线程跑 RTMP；url 所有权转移给该线程，结束时 delete。
        pushThread = std::thread(start, url);
    }
    env->ReleaseStringUTFChars(path_, path);
}

/**
 * native_1pushVideo：JNI jbyteArray → 裸指针 → encodeVideo。
 * frame_type 对应 Java LiveFrameFormat（NV21/I420）。
 */
LIVE_PUSHER_FUNC(void, native_1pushVideo, jbyteArray yuv, jint frame_type) {
    // 未连接或未 init：直接丢弃，避免未发布就堆积编码包。
    if (!videoStream || !isPushing) {
        return;
    }
    jbyte *yuv_plane = env->GetByteArrayElements(yuv, JNI_FALSE);
    videoStream->encodeVideo(yuv_plane, frame_type);
    // 第三个参数 0：写回 Java 数组并释放 native 锁定（若仅读也可用 JNI_ABORT，此处双保险）。
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

/** Java AudioRecord 缓冲区长度 = getInputSamples() * 每采样字节数（PCM 16-bit 为 2；立体声再乘声道数） */
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

/** native_1stop：置停止标志并关闭队列接收；发送线程因 isPushing=false 退出循环（不一定立刻断 TCP） */
LIVE_PUSHER_FUNC(void, native_1stop) {
    LOGI("native stop...");
    stopRequested = true;
    isPushing = false;
    packets.setRunning(false);
}

/** native_1release：释放 GlobalRef 与编码器；典型 Activity onDestroy；与 init 成对 */
LIVE_PUSHER_FUNC(void, native_1release) {
    LOGI("native release...");
    stopRequested = true;
    isPushing = false;
    packets.setRunning(false);
    jobject callbackObj = nullptr;
    {
        // 与 init / throwErrToJava 互斥：先取出指针再删引用，避免并发 CallVoidMethod 使用已释放对象。
        std::lock_guard<std::mutex> lock(callbackMutex);
        callbackObj = jobject_error;
        jobject_error = nullptr;
    }
    if (callbackObj != nullptr) {
        env->DeleteGlobalRef(callbackObj);
    }
    delete videoStream;
    videoStream = nullptr;
    delete audioStream;
    audioStream = nullptr;
}
