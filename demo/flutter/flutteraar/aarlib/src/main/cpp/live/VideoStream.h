#ifndef VIDEOSTREAM_H
#define VIDEOSTREAM_H

#include <inttypes.h>
#include <mutex>
#include "rtmp/rtmp.h"
#include "include/x264/x264.h"

/**
 * 视频编码与 RTMP 视频包封装模块：
 * - 使用 x264 将 I420/NV21 转码为 H264；
 * - 生成 SPS/PPS 与普通帧包；
 * - 回调输出 RTMP 视频包。
 */
class VideoStream {
    typedef void (*VideoCallback)(RTMPPacket *packet);

private:
    std::mutex m_mutex;

    int m_frameLen;
    x264_t *videoCodec = 0;
    x264_picture_t *pic_in = 0;

    VideoCallback videoCallback;

    void sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len);

    void sendFrame(int type, uint8_t *payload, int i_payload);

public:
    /**
     * 构造函数。
     */
    VideoStream();

    /**
     * 析构函数，释放 x264 资源。
     */
    ~VideoStream();

    /**
     * 配置 H264 编码参数。
     */
    int setVideoEncInfo(int width, int height, int fps, int bitrate);

    /**
     * 编码一帧视频并回调 RTMP 包。
     */
    void encodeVideo(int8_t *data, int camera_type);

    /**
     * 设置编码后 RTMP 包回调。
     */
    void setVideoCallback(VideoCallback videoCallback);

};

#endif
