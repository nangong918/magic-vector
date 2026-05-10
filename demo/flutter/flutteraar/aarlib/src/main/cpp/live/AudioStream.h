#ifndef AUDIOSTREAM_H
#define AUDIOSTREAM_H

/**
 * @file AudioStream.h
 * @brief PCM → FAAC(AAC-LC) → FLV 音频 Tag → RTMPPacket。
 *
 * 【数据路径】Java AudioRecord PCM → native_1pushAudio → encodeData → audioCallback(RtmpPusher::callback)。
 */

#include "rtmp/rtmp.h"
#include "include/faac/faac.h"
#include <sys/types.h>

/**
 * 音频编码与 RTMP 音频包封装模块：
 * - 使用 FAAC 将 PCM 编码为 AAC；
 * - 生成 AAC AudioSpecificConfig；
 * - 回调输出 RTMP 音频包。
 */
class AudioStream {
    typedef void (*AudioCallback)(RTMPPacket *packet);

private:
    AudioCallback audioCallback; /**< 编码完成后回调：由 RtmpPusher 设为 callback，负责打时间戳+入队 */
    int m_channels;           /**< 声道数：影响 FLV 首字节 0xAE（单）/0xAF（双） */
    faacEncHandle m_audioCodec = 0; /**< FAAC 编码器实例 */
    u_long m_inputSamples;    /**< 每次 encodeData 需要的采样点数（整帧）；与 Java 缓冲长度对齐 */
    u_long m_maxOutputBytes; /**< FAAC 单帧 AAC 输出上限；用于分配 m_buffer */
    u_char *m_buffer = 0;    /**< AAC 编码输出临时缓冲 */

public:
    /**
     * 构造函数。
     */
    AudioStream();

    /**
     * 析构函数，释放编码器与缓冲区。
     */
    ~AudioStream();

    /** 打开 FAAC；返回 faacEncSetConfiguration 是否成功（<0 失败） */
    int setAudioEncInfo(int samplesInHZ, int channels);

    /** 注册编码完成回调（通常为 RtmpPusher::callback） */
    void setAudioCallback(AudioCallback audioCallback);

    /** Java 每帧 PCM 字节数 ≈ getInputSamples() × 声道数 × 2（16-bit）；具体以录音配置为准 */
    int getInputSamples() const;

    /** PCM 一帧 → AAC Raw → FLV 音频包（AACPacketType=1） */
    void encodeData(int8_t *data);

    /** AAC Sequence Header（ASC，AACPacketType=0）；RTMP 连通后必须先于裸帧发送 */
    RTMPPacket *getAudioTag();

};


#endif
