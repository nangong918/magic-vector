#ifndef AUDIOSTREAM_H
#define AUDIOSTREAM_H

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
    AudioCallback audioCallback;
    int m_channels;
    faacEncHandle m_audioCodec = 0;
    u_long m_inputSamples;
    u_long m_maxOutputBytes;
    u_char *m_buffer = 0;

public:
    /**
     * 构造函数。
     */
    AudioStream();

    /**
     * 析构函数，释放编码器与缓冲区。
     */
    ~AudioStream();

    /**
     * 配置 AAC 编码参数。
     */
    int setAudioEncInfo(int samplesInHZ, int channels);

    /**
     * 设置编码后 RTMP 包回调。
     */
    void setAudioCallback(AudioCallback audioCallback);

    /**
     * 获取编码器期望输入采样点数。
     */
    int getInputSamples() const;

    /**
     * 编码一帧 PCM 数据并回调 RTMP 音频包。
     */
    void encodeData(int8_t *data);

    /**
     * 生成 AAC Sequence Header 包。
     */
    RTMPPacket *getAudioTag();

};


#endif
