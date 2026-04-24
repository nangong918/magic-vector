#include "ff_rtmp_pusher.h"
#include <android/log.h>

#define PUSH_TAG "ff_rtmp_pusher"
#define FFLOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO, PUSH_TAG, FORMAT, ##__VA_ARGS__)
#define FFLOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, PUSH_TAG, FORMAT, ##__VA_ARGS__)

int FFRtmpPusher::open(const char *inputPath, const char *outputPath) {
    int ret;

    avformat_network_init();
    ret = avformat_open_input(&inFormatCtx, inputPath, nullptr, nullptr);
    if (ret < 0) {
        FFLOGE("avformat_open_input err=%d", ret);
        return ret;
    }
    avformat_find_stream_info(inFormatCtx, nullptr);
    av_dump_format(inFormatCtx, 0, inputPath, 0);
    ret = avformat_alloc_output_context2(&outFormatCtx, nullptr, "flv", outputPath);
    if (ret < 0 || !outFormatCtx) {
        FFLOGE("alloc format_context err=%d", ret);
        return ret;
    }

    for (int i = 0; i < inFormatCtx->nb_streams; ++i) {
        AVStream *in_stream = inFormatCtx->streams[i];
        const auto *codec = avcodec_find_encoder(in_stream->codecpar->codec_id);
        AVStream *out_stream = avformat_new_stream(outFormatCtx, codec);
        avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
        out_stream->codecpar->codec_tag = 0;

        if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
        } else if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (audio_index == -1) {
                audio_index = i;
            }
        }
    }

    if (!(outFormatCtx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open2(&outFormatCtx->pb, outputPath, AVIO_FLAG_WRITE, nullptr, nullptr);
        if (ret < 0) {
            FFLOGE("avio open error=%d", ret);
            return ret;
        }
    }

    ret = avformat_write_header(outFormatCtx, nullptr);
    if (ret < 0) {
        FFLOGE("avformat_write_header err=%d", ret);
    }
    return ret;
}

void rescale(AVFormatContext *in_format_ctx, AVFormatContext *out_format_ctx, AVPacket *packet) {
    AVStream *in_stream = in_format_ctx->streams[packet->stream_index];
    AVStream *out_stream = out_format_ctx->streams[packet->stream_index];

    if (in_stream->time_base.num == out_stream->time_base.num
            && in_stream->time_base.den == out_stream->time_base.den) {
        packet->pos = -1;
        return;
    }

    packet->pts = av_rescale_q(packet->pts, in_stream->time_base, out_stream->time_base);
    packet->dts = av_rescale_q(packet->dts, in_stream->time_base, out_stream->time_base);
    packet->duration = av_rescale_q(packet->duration, in_stream->time_base, out_stream->time_base);
    packet->pos = -1;
}

int FFRtmpPusher::push() {
    int ret;
    int64_t startTime = av_gettime();

    while (true) {
        ret = av_read_frame(inFormatCtx, &packet);
        if (ret < 0) {
            FFLOGE("av_read_frame err=%d", ret);
            break;
        }

        if (packet.stream_index != video_index && packet.stream_index != audio_index) {
            av_packet_unref(&packet);
            continue;
        }

        AVRational time_base = inFormatCtx->streams[packet.stream_index]->time_base;
        int64_t pts_time = av_rescale_q(packet.pts, time_base, AV_TIME_BASE_Q);
        int64_t cur_time = av_gettime() - startTime;
        if (pts_time > cur_time) {
            av_usleep(static_cast<unsigned int>(pts_time - cur_time));
        }

        rescale(inFormatCtx, outFormatCtx, &packet);

        ret = av_interleaved_write_frame(outFormatCtx, &packet);
        if (ret < 0) {
            FFLOGE("write frame err=%d", ret);
            break;
        }

        av_packet_unref(&packet);
    }

    return ret;
}

void FFRtmpPusher::close() {
    if (outFormatCtx) {
        av_write_trailer(outFormatCtx);
        if (!(outFormatCtx->oformat->flags & AVFMT_NOFILE) && outFormatCtx->pb) {
            avio_closep(&outFormatCtx->pb);
        }
        avformat_free_context(outFormatCtx);
        outFormatCtx = nullptr;
    }
    if (inFormatCtx) {
        avformat_close_input(&inFormatCtx);
        inFormatCtx = nullptr;
    }
}
