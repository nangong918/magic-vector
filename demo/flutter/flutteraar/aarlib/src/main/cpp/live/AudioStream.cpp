
#include <cstring>
#include "AudioStream.h"
#include "PushInterface.h"

/**
 * @file AudioStream.cpp（注释合并说明）
 * - 保留原先「第1步～第4步」PCM→AAC→FLV→RTMPPacket 的拆解注释；
 * - 保留原先 FLV Audio Tag 首字节、次字节位含义说明；
 * - 下文凡标注「原注释保留」即为您原先文档里的表述，未删减。
 */

AudioStream::AudioStream() {
    LOGI("AudioStream created");
}

void AudioStream::setAudioCallback(AudioCallback callback) {
    // 保存上层传入的回调：编码完成后把 RTMPPacket 交给 RtmpPusher::callback → 入队。
    audioCallback = callback;
}

int AudioStream::setAudioEncInfo(int samplesInHZ, int channels) {
    LOGI("setAudioEncInfo, sampleRate=%d, channels=%d", samplesInHZ, channels);
    m_channels = channels;
    // faacEncOpen：输入采样率、声道数；输出 m_inputSamples（每帧需喂入的采样数）、m_maxOutputBytes（AAC 输出上限）
    m_audioCodec = faacEncOpen(static_cast<unsigned long>(samplesInHZ),
            static_cast<unsigned int>(channels),
            &m_inputSamples,
            &m_maxOutputBytes);
    m_buffer = new u_char[m_maxOutputBytes];

    faacEncConfigurationPtr config = faacEncGetCurrentConfiguration(m_audioCodec);
    config->mpegVersion = MPEG4;           // AAC 基于 MPEG-4 音频部分
    config->aacObjectType = LOW;           // AAC-LC（LOW），RTMP/FLV 常见兼容配置
    config->inputFormat = FAAC_INPUT_16BIT; // 与 Java AudioRecord PCM_16BIT 对齐
    // outputFormat=0：原始 AAC 帧，由本类自行包 FLV 头（非 ADTS 封装输出）
    config->outputFormat = 0;
    int ret = faacEncSetConfiguration(m_audioCodec, config);
    LOGI("setAudioEncInfo done, ret=%d, inputSamples=%lu, maxOutputBytes=%lu", ret, m_inputSamples, m_maxOutputBytes);
    return ret;
}

int AudioStream::getInputSamples() const {
    // FAAC 返回的「每帧输入采样数」：Java 侧应按「采样数 × 每采样字节 × 声道数」准备 PCM 缓冲。
    return static_cast<int>(m_inputSamples);
}

RTMPPacket *AudioStream::getAudioTag() {
    LOGI("getAudioTag");
    u_char *buf;
    u_long len;
    // 向 FAAC 索取 AudioSpecificConfig 二进制，供解码器初始化（与 ADTS 流中的 config 同义信息）
    // AudioSpecificConfig：与 AAC-LC 码流中描述采样率/声道/对象类型的头信息等价，拉流端据此初始化解码器。
    faacEncGetDecoderSpecificInfo(m_audioCodec, &buf, &len);
    int bodySize = static_cast<int>(2 + len);
    auto *packet = new RTMPPacket();
    RTMPPacket_Alloc(packet, bodySize);
    // 原注释保留：第1字节 SoundFormat(10=AAC) + 采样率/位深/声道 组合；双声道 0xAF，单声道 0xAE
    packet->m_body[0] = 0xAF;
    if (m_channels == 1) {
        packet->m_body[0] = 0xAE;
    }
    // 原注释保留：第2字节 AACPacketType=0x00 表示 Sequence Header（非原始 AAC 帧）
    packet->m_body[1] = 0x00;

    memcpy(&packet->m_body[2], buf, len);

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize = bodySize;
    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    // 原注释保留：音频在 RTMP 中常用 chunk stream id；m_nChannel 为 RTMP 逻辑通道号，非 PCM 声道数。
    packet->m_nChannel = 0x11;
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return packet;
}

/**
 * 音频编码并封装为 RTMP 音频包，通过回调推流
 *
 * @param data PCM 原始音频数据（int8_t 指针，实际为 16-bit 采样，通过 reinterpret_cast 转为 int32_t 传入编码器）
 *
 * 流程（原注释保留）：
 *   PCM 数据 → FAAC 软编码 → AAC 帧 → 添加 FLV Audio Tag Header → 封装 RTMPPacket → 推流回调
 */
void AudioStream::encodeData(int8_t *data) {
    // ========== 第1步：FAAC 软编码，将 PCM 编码为 AAC ==========
    // faacEncEncode: 输入 PCM，输出 AAC 帧到 m_buffer，返回编码后的字节数
    int byteLen = faacEncEncode(
            m_audioCodec,                              // FAAC 编码器句柄
            reinterpret_cast<int32_t *>(data),          // 输入：PCM 采样数据（16-bit，按 32-bit 传入以匹配接口）
            static_cast<unsigned int>(m_inputSamples),  // 输入：每次编码的采样数
            m_buffer,                                   // 输出：AAC 编码数据缓冲区
            static_cast<unsigned int>(m_maxOutputBytes) // 输出缓冲区的最大容量
    );

    if (byteLen > 0) {
        LOGI("encodeData success, byteLen=%d", byteLen);

        // ========== 第2步：构建 FLV Audio Tag Header + AAC 负载 ==========
        // FLV Audio Tag 格式：[Header 2字节] + [AAC 负载]
        int bodySize = 2 + byteLen;                     // 总长度 = 2字节头 + AAC帧长度

        auto *packet = new RTMPPacket();
        RTMPPacket_Alloc(packet, bodySize);

        // --- FLV Audio Tag Header 第1字节：SoundFormat + SoundRate + SoundSize + SoundType ---
        // 0xAF = 1010 1111  = AAC(10) + 44.1kHz(10) + 16-bit(1) + Stereo(1)
        // 0xAE = 1010 1110  = AAC(10) + 44.1kHz(10) + 16-bit(1) + Mono(0)
        packet->m_body[0] = 0xAF;                       // 默认双声道
        if (m_channels == 1) {
            packet->m_body[0] = 0xAE;                   // 单声道改为 0xAE
        }

        // --- FLV Audio Tag Header 第2字节：AACPacketType ---
        // 0x00 = AAC序列头（AudioSpecificConfig）
        // 0x01 = AAC原始帧数据
        packet->m_body[1] = 0x01;                         // AAC 原始音频帧

        // --- 拷贝 AAC 编码数据到 FLV Tag Body ---
        // m_body[0~1] 是 Header，从 m_body[2] 开始存放 AAC 数据
        memcpy(&packet->m_body[2], m_buffer, static_cast<size_t>(byteLen));

        // ========== 第3步：填充 RTMPPacket 元信息，准备推流 ==========
        packet->m_hasAbsTimestamp = 0;                   // 使用相对时间戳（非绝对时间）
        packet->m_nBodySize = bodySize;                  // 包体大小 = Header(2) + AAC数据
        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;  // 包类型：音频
        packet->m_nChannel = 0x11;                       // 音频声道标识（用于 RTMP 流复用）
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE;   // RTMP Chunk 头类型（大包模式）

        // ========== 第4步：通过回调将 RTMP 包推入发送队列 ==========
        audioCallback(packet);
    }
}

AudioStream::~AudioStream() {
    LOGI("AudioStream destroy");
    delete m_buffer;
    m_buffer = nullptr;
    if (m_audioCodec) {
        faacEncClose(m_audioCodec);
        m_audioCodec = nullptr;
    }
}
