#include "ff_rtmp_pusher.h"
#include <android/log.h>
#include <cstring>
#include <vector>

#define PUSH_TAG "ff_rtmp_pusher"
#define FFLOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO, PUSH_TAG, FORMAT, ##__VA_ARGS__)
#define FFLOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, PUSH_TAG, FORMAT, ##__VA_ARGS__)

static bool starts_with(const char *value, const char *prefix) {
    if (value == nullptr || prefix == nullptr) {
        return false;
    }
    size_t prefix_len = strlen(prefix);
    return strncmp(value, prefix, prefix_len) == 0;
}

static bool is_rtsp_output(const char *outputPath) {
    return starts_with(outputPath, "rtsp://");
}

static const char *detect_output_format(const char *outputPath) {
    if (is_rtsp_output(outputPath)) {
        return "rtsp";
    }
    if (starts_with(outputPath, "rtmp://")
            || starts_with(outputPath, "rtmps://")
            || starts_with(outputPath, "rtmpt://")) {
        return "flv";
    }
    // 让 FFmpeg 根据 URL 自动猜测格式
    return nullptr;
}

int FFRtmpPusher::open(const char *inputPath, const char *outputPath) {
    int ret;
    FFLOGI("open, input=%s, output=%s", inputPath, outputPath);

    avformat_network_init();
    ret = avformat_open_input(&inFormatCtx, inputPath, nullptr, nullptr);
    if (ret < 0) {
        FFLOGE("avformat_open_input err=%d", ret);
        return ret;
    }
    avformat_find_stream_info(inFormatCtx, nullptr);
    av_dump_format(inFormatCtx, 0, inputPath, 0);
    const char *format_name = detect_output_format(outputPath);
    ret = avformat_alloc_output_context2(&outFormatCtx, nullptr, format_name, outputPath);
    if (ret < 0 || !outFormatCtx) {
        FFLOGE("alloc format_context err=%d", ret);
        return ret;
    }
    FFLOGI("open output format=%s", format_name == nullptr ? "auto" : format_name);

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

    if (is_rtsp_output(outputPath)) {
        // RTSP 推流优先走 TCP，减少弱网下 UDP 丢包导致的失败。
        av_dict_set(&muxerOptions, "rtsp_transport", "tcp", 0);
        av_dict_set(&muxerOptions, "muxdelay", "0.1", 0);
    }

    ret = avformat_write_header(outFormatCtx, &muxerOptions);
    if (ret < 0) {
        FFLOGE("avformat_write_header err=%d", ret);
    }
    av_dict_free(&muxerOptions);
    FFLOGI("open finished, ret=%d", ret);
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

    if (packet->pts != AV_NOPTS_VALUE) {
        packet->pts = av_rescale_q(packet->pts, in_stream->time_base, out_stream->time_base);
    }
    if (packet->dts != AV_NOPTS_VALUE) {
        packet->dts = av_rescale_q(packet->dts, in_stream->time_base, out_stream->time_base);
    }
    if (packet->duration > 0) {
        packet->duration = av_rescale_q(packet->duration, in_stream->time_base, out_stream->time_base);
    }
    packet->pos = -1;
}

int FFRtmpPusher::push() {
    int ret = 0;
    int64_t startTime = av_gettime();
    std::vector<int64_t> firstPts(inFormatCtx->nb_streams, AV_NOPTS_VALUE);
    std::vector<int64_t> firstDts(inFormatCtx->nb_streams, AV_NOPTS_VALUE);
    std::vector<int64_t> lastOutDts(inFormatCtx->nb_streams, AV_NOPTS_VALUE);
    FFLOGI("push start");

    while (true) {
        ret = av_read_frame(inFormatCtx, &packet);
        if (ret < 0) {
            if (ret == AVERROR_EOF) {
                FFLOGI("av_read_frame EOF, treat as normal finish");
                ret = 0;
            } else {
                FFLOGE("av_read_frame err=%d", ret);
            }
            break;
        }

        if (packet.stream_index != video_index && packet.stream_index != audio_index) {
            av_packet_unref(&packet);
            continue;
        }

        int streamIndex = packet.stream_index;
        AVRational time_base = inFormatCtx->streams[packet.stream_index]->time_base;

        // 归一化时间戳（从 0 开始），避免文件原始时间戳跳变导致拉流端解码异常。
        if (packet.pts != AV_NOPTS_VALUE) {
            if (firstPts[streamIndex] == AV_NOPTS_VALUE) {
                firstPts[streamIndex] = packet.pts;
            }
            packet.pts -= firstPts[streamIndex];
            if (packet.pts < 0) {
                packet.pts = 0;
            }
        }
        if (packet.dts != AV_NOPTS_VALUE) {
            if (firstDts[streamIndex] == AV_NOPTS_VALUE) {
                firstDts[streamIndex] = packet.dts;
            }
            packet.dts -= firstDts[streamIndex];
            if (packet.dts < 0) {
                packet.dts = 0;
            }
        }
        if (packet.pts != AV_NOPTS_VALUE
                && packet.dts != AV_NOPTS_VALUE
                && packet.pts < packet.dts) {
            packet.pts = packet.dts;
        }

        int64_t syncTs = packet.dts != AV_NOPTS_VALUE ? packet.dts : packet.pts;
        if (syncTs != AV_NOPTS_VALUE) {
            int64_t mediaTimeUs = av_rescale_q(syncTs, time_base, AV_TIME_BASE_Q);
            int64_t elapsedUs = av_gettime() - startTime;
            int64_t waitUs = mediaTimeUs - elapsedUs;
            // 避免单次等待过久导致 RTSP 服务端判定超时断开。
            if (waitUs > 0) {
                if (waitUs > 200000) {
                    waitUs = 200000;
                }
                av_usleep(static_cast<unsigned int>(waitUs));
            }
        }

        rescale(inFormatCtx, outFormatCtx, &packet);
        if (packet.dts != AV_NOPTS_VALUE) {
            if (lastOutDts[streamIndex] != AV_NOPTS_VALUE
                    && packet.dts <= lastOutDts[streamIndex]) {
                packet.dts = lastOutDts[streamIndex] + 1;
                if (packet.pts != AV_NOPTS_VALUE && packet.pts < packet.dts) {
                    packet.pts = packet.dts;
                }
            }
            lastOutDts[streamIndex] = packet.dts;
        }

        ret = av_interleaved_write_frame(outFormatCtx, &packet);
        if (ret < 0) {
            FFLOGE("write frame err=%d", ret);
            av_packet_unref(&packet);
            break;
        }

        av_packet_unref(&packet);
    }

    FFLOGI("push finish, ret=%d", ret);
    return ret;
}

void FFRtmpPusher::close() {
    FFLOGI("close");
    av_dict_free(&muxerOptions);
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
