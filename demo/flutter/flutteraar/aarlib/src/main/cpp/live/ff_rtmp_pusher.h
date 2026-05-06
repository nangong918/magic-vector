#ifndef AARLIB_FF_RTMP_PUSHER_H
#define AARLIB_FF_RTMP_PUSHER_H

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
    AVFormatContext *inFormatCtx = nullptr;
    AVFormatContext *outFormatCtx = nullptr;
    AVDictionary *muxerOptions = nullptr;

    AVPacket packet;
    int video_index = -1;
    int audio_index = -1;

public:
    /**
     * 打开输入与输出。
     */
    int open(const char *inputPath, const char *outputPath);

    /**
     * 开始转推。
     */
    int push();

    /**
     * 关闭并释放资源。
     */
    void close();
};

#endif
