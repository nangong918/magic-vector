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

class FFRtmpPusher {
private:
    AVFormatContext *inFormatCtx = nullptr;
    AVFormatContext *outFormatCtx = nullptr;

    AVPacket packet;
    int video_index = -1;
    int audio_index = -1;

public:
    int open(const char *inputPath, const char *outputPath);

    int push();

    void close();
};

#endif
