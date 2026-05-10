#ifndef AARLIB_FF_RTMP_PUSHER_H
#define AARLIB_FF_RTMP_PUSHER_H

/**
 * @file ff_rtmp_pusher.h
 * @brief FFmpeg remux：输入任意 libavformat 支持的 URL/文件，输出 rtmp:// 或 rtsp://（不重编码）。
 *
 * 【成员职责】inFormatCtx 读包；outFormatCtx 写包；packet 复用读缓冲；video_index/audio_index 过滤轨。
 */

#ifdef __cplusplus
extern "C" {
#endif
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavutil/time.h"
#ifdef __cplusplus
}
#endif

/**
 * FFmpeg 文件转推器：
 * - 打开输入媒体；
 * - 初始化 RTMP(FLV)/RTSP 输出；
 * - 循环转推音视频包。
 */
class FFRtmpPusher {
private:
    AVFormatContext *inFormatCtx = nullptr;  /**< 输入容器：open_input / read_frame */
    AVFormatContext *outFormatCtx = nullptr; /**< 输出容器：write_header / interleaved_write_frame */
    AVDictionary *muxerOptions = nullptr;   /**< write_header 可选参数：如 RTSP 走 TCP */

    AVPacket packet;      /**< 栈上复用：每次 av_read_frame 填充，写完 write_frame 后 unref */
    int video_index = -1; /**< 选中的视频轨 stream_index；-1 表示无视频或未发现 */
    int audio_index = -1; /**< 选中的首条音频轨；其余音轨在 push 中丢弃 */

public:
    /** 打开输入、创建输出轨、写头部；失败返回 libav 负错误码 */
    int open(const char *inputPath, const char *outputPath);

    /** 读帧、时间戳修正、按媒体时钟节拍 sleep、写入输出；读完返回 0 或错误码 */
    int push();

    /** 写尾部、关闭 IO、释放上下文 */
    void close();
};

#endif
