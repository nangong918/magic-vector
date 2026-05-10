#ifndef VIDEOSTREAM_H
#define VIDEOSTREAM_H

/**
 * @file VideoStream.h
 * @brief YUV(I420/NV21) → x264 → H.264 NAL → FLV AVC 封装 → RTMPPacket 回调。
 *
 * 【并发】m_mutex 串行化 setVideoEncInfo 与 encodeVideo，避免分辨率变更与编码交错。
 */

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
    std::mutex m_mutex; /**< 保护 setVideoEncInfo / encodeVideo：防止 x264 句柄与 pic_in 并发访问 */

    int m_frameLen;              /**< width×height：Y 平面字节数；NV21/I420 偏移计算基准 */
    x264_t *videoCodec = 0;      /**< x264 编码器；setVideoEncInfo 内创建，析构关闭 */
    x264_picture_t *pic_in = 0; /**< I420 输入图像：三平面指针由 x264_picture_alloc 分配 */

    VideoCallback videoCallback; /**< 产出 RTMPPacket 后调用：一般为 RtmpPusher::callback */

    /** 将 SPS/PPS 封装为 FLV AVC sequence header（AVCPacketType=0） */
    void sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len);

    /** 将一帧 NAL（已含 Annex-B）封装为 FLV AVC NALU（AVCPacketType=1） */
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
     * @brief 初始化或重置 x264（分辨率/码率变化时调用）。
     * @param bitrate 视频目标码率，单位与 Java 一致为 bps（内部会 /1024 转为 kbps）
     */
    int setVideoEncInfo(int width, int height, int fps, int bitrate);

    /**
     * @brief 编码一帧；camera_type：1=NV21，2=I420/YV12（与 Java 约定一致）。
     */
    void encodeVideo(int8_t *data, int camera_type);

    /**
     * 设置编码后 RTMP 包回调。
     */
    void setVideoCallback(VideoCallback videoCallback);

};

#endif
