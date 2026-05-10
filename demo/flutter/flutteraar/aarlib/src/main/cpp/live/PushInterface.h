#ifndef PUSHINTERFACE_H
#define PUSHINTERFACE_H

#include <android/log.h>

/**
 * Live native 公共定义：
 * - 统一日志宏；
 * - 统一错误码常量。
 *
 * 错误码由 RtmpPusher.cpp 在 JNI 中通过 throwErrToJava() 传给
 * LivePusherBridge.errorFromNative(int)，再由 Java 映射成对用户友好的文案。
 */
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"LivePushSDK",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"LivePushSDK",__VA_ARGS__)

/** x264_encoder_open / x264_param_apply 等失败 */
const int ERROR_VIDEO_ENCODER_OPEN   = 0x01;
/** 预留：单帧编码失败（当前工程较少单独上报） */
const int ERROR_VIDEO_ENCODE         = 0x02;
/** faacEncOpen / faacEncSetConfiguration 失败 */
const int ERROR_AUDIO_ENCODER_OPEN   = 0x03;
/** 预留：音频编码失败 */
const int ERROR_AUDIO_ENCODE         = 0x04;
/** RTMP_Connect 失败（TCP 连不上服务器或握手失败） */
const int ERROR_RTMP_CONNECT         = 0x05;
/** RTMP_ConnectStream 失败（连接上了但未正确 publish/instantiate 流） */
const int ERROR_RTMP_CONNECT_STREAM  = 0x06;
/** RTMP_SendPacket 失败（发送中途断开或参数异常） */
const int ERROR_RTMP_SEND_PACKET     = 0x07;

#endif
