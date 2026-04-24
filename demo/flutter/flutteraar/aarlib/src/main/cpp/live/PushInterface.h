#ifndef PUSHINTERFACE_H
#define PUSHINTERFACE_H

#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"LivePushSDK",__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,"LivePushSDK",__VA_ARGS__)

const int ERROR_VIDEO_ENCODER_OPEN   = 0x01;
const int ERROR_VIDEO_ENCODE         = 0x02;
const int ERROR_AUDIO_ENCODER_OPEN   = 0x03;
const int ERROR_AUDIO_ENCODE         = 0x04;
const int ERROR_RTMP_CONNECT         = 0x05;
const int ERROR_RTMP_CONNECT_STREAM  = 0x06;
const int ERROR_RTMP_SEND_PACKET     = 0x07;

#endif
