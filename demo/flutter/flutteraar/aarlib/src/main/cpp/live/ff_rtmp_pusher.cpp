/**
 * @file ff_rtmp_pusher.cpp
 * @brief FFmpeg 解封装输入 → 可选时间戳修正 → 交织写入 RTMP 或 RTSP（不重编码，remux）。
 *
 * 【open 流程要点】（与代码顺序对应）
 * - avformat_open_input / find_stream_info：解析容器与流；
 * - 为每条输入轨 new 输出轨并 copy codecpar（不重编码）；
 * - video_index / audio_index：push 里只转发这两条轨；
 * - RTSP 输出设置 rtsp_transport=tcp，减轻 UDP 丢包。
 *
 * 【push 流程要点】
 * - 时间戳归一化：每轨各自减去首帧 PTS/DTS，使时间从 0 起；
 * - 节拍：syncTs 换算成微秒与 wallclock 比较，不够睡 waitUs（封顶 200ms）；
 * - 单调 DTS：若 rescale 后 dts 仍倒退，强制 +1，防止 muxer 报错。
 */

#include "ff_rtmp_pusher.h"

#include <android/log.h>
#include <cstring>
#include <vector>

#define PUSH_TAG "ff_rtmp_pusher"
#define FFLOGI(FORMAT, ...) __android_log_print(ANDROID_LOG_INFO, PUSH_TAG, FORMAT, ##__VA_ARGS__)
#define FFLOGE(FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR, PUSH_TAG, FORMAT, ##__VA_ARGS__)

/** 判断字符串 value 是否以 prefix 开头（用于识别 rtmp:// / rtsp://）。 */
static bool starts_with(const char *value, const char *prefix) {
    if (value == nullptr || prefix == nullptr) {
        return false;
    }
    size_t prefix_len = strlen(prefix);
    return strncmp(value, prefix, prefix_len) == 0;
}

/** 输出是否为 RTSP URL（决定 muxer 与 TCP 选项）。 */
static bool is_rtsp_output(const char *outputPath) {
    return starts_with(outputPath, "rtsp://");
}

/** 根据输出 URL 推断封装格式名：传给 avformat_alloc_output_context2 的 short_name。 */
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

/**
 * @brief 打开输入文件/URL，创建输出上下文并 write_header。
 */
int FFRtmpPusher::open(const char *inputPath, const char *outputPath) {
    int ret;
    FFLOGI("open, input=%s, output=%s", inputPath, outputPath);

    avformat_network_init(); // 初始化 Socket，允许 http/rtmp/rtsp 等网络协议

    ret = avformat_open_input(&inFormatCtx, inputPath, nullptr, nullptr); // 探测容器头，建立输入上下文
    if (ret < 0) {
        FFLOGE("avformat_open_input err=%d", ret);
        return ret;
    }
    avformat_find_stream_info(inFormatCtx, nullptr); // 解析流、解码器参数、时长等元数据
    av_dump_format(inFormatCtx, 0, inputPath, 0);    // 调试：打印输入格式信息到 log

    const char *format_name = detect_output_format(outputPath);
    ret = avformat_alloc_output_context2(&outFormatCtx, nullptr, format_name, outputPath); // 创建输出 muxer
    if (ret < 0 || !outFormatCtx) {
        FFLOGE("alloc format_context err=%d", ret);
        return ret;
    }
    FFLOGI("open output format=%s", format_name == nullptr ? "auto" : format_name);

    for (int i = 0; i < inFormatCtx->nb_streams; ++i) {
        AVStream *in_stream = inFormatCtx->streams[i];
        const auto *codec = avcodec_find_encoder(in_stream->codecpar->codec_id); // remux 仅需占位 codec，参数靠 copy
        AVStream *out_stream = avformat_new_stream(outFormatCtx, codec);       // 为输出容器增加一条流
        avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);      // 不重编码：直接拷贝码流描述
        out_stream->codecpar->codec_tag = 0;                                     // 让 muxer 自行设置 fourcc/tag

        if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i; // 覆盖策略：最后一条视频轨生效（多数文件仅一条视频）
        } else if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (audio_index == -1) {
                audio_index = i; // 只取第一条音频轨，忽略副音轨/解说轨
            }
        }
    }

    if (!(outFormatCtx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open2(&outFormatCtx->pb, outputPath, AVIO_FLAG_WRITE, nullptr, nullptr); // 打开输出 URL/文件
        if (ret < 0) {
            FFLOGE("avio open error=%d", ret);
            return ret;
        }
    }

    if (is_rtsp_output(outputPath)) {
        // RTSP 推流优先走 TCP，减少弱网下 UDP 丢包导致的失败。
        av_dict_set(&muxerOptions, "rtsp_transport", "tcp", 0);
        av_dict_set(&muxerOptions, "muxdelay", "0.1", 0); // 轻微缓冲，降低 RTP 突发
    }

    ret = avformat_write_header(outFormatCtx, &muxerOptions); // 写全局头（FLV/RTSP SDP 等）
    if (ret < 0) {
        FFLOGE("avformat_write_header err=%d", ret);
    }
    av_dict_free(&muxerOptions); // write_header 可能消费并清空选项；此处再释放防泄漏
    FFLOGI("open finished, ret=%d", ret);
    return ret;
}

/**
 * @brief 若输入输出 time_base 不同，把 pts/dts/duration 换算到输出轨时间基。
 * pos 置 -1 让 muxer 自行决定文件偏移。
 */
void rescale(AVFormatContext *in_format_ctx, AVFormatContext *out_format_ctx, AVPacket *packet) {
    AVStream *in_stream = in_format_ctx->streams[packet->stream_index];
    AVStream *out_stream = out_format_ctx->streams[packet->stream_index];

    if (in_stream->time_base.num == out_stream->time_base.num
            && in_stream->time_base.den == out_stream->time_base.den) {
        packet->pos = -1; // 时间基相同则无需换算；pos=-1 表示由 muxer 写偏移
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

/**
 * @brief 循环读 AVPacket，按「媒体时钟」sleep，再交错写入输出。
 */
int FFRtmpPusher::push() {
    int ret = 0;
    int64_t startTime = av_gettime(); // 墙钟起点：用于按录制时间节拍节流
    std::vector<int64_t> firstPts(inFormatCtx->nb_streams, AV_NOPTS_VALUE); // 每轨首 PTS，用于归零
    std::vector<int64_t> firstDts(inFormatCtx->nb_streams, AV_NOPTS_VALUE); // 每轨首 DTS，用于归零
    std::vector<int64_t> lastOutDts(inFormatCtx->nb_streams, AV_NOPTS_VALUE); // 输出侧单调 DTS 校正
    FFLOGI("push start");

    while (true) {
        ret = av_read_frame(inFormatCtx, &packet); // 读一条压缩包（音频或视频）
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
            av_packet_unref(&packet); // 非选定轨：立即丢弃，避免写错轨
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
            packet.pts = packet.dts; // 保证 PTS 不小于 DTS，避免部分 muxer 报错
        }

        int64_t syncTs = packet.dts != AV_NOPTS_VALUE ? packet.dts : packet.pts; // 节拍优先用 DTS
        if (syncTs != AV_NOPTS_VALUE) {
            int64_t mediaTimeUs = av_rescale_q(syncTs, time_base, AV_TIME_BASE_Q); // 媒体时间 → 微秒
            int64_t elapsedUs = av_gettime() - startTime; // 已消耗墙钟时间
            int64_t waitUs = mediaTimeUs - elapsedUs;     // 距离「该播放时刻」还需等待多久
            // 避免单次等待过久导致 RTSP 服务端判定超时断开。
            if (waitUs > 0) {
                if (waitUs > 200000) {
                    waitUs = 200000;
                }
                av_usleep(static_cast<unsigned int>(waitUs));
            }
        }

        rescale(inFormatCtx, outFormatCtx, &packet); // 换算到输出轨 time_base
        if (packet.dts != AV_NOPTS_VALUE) {
            if (lastOutDts[streamIndex] != AV_NOPTS_VALUE
                    && packet.dts <= lastOutDts[streamIndex]) {
                packet.dts = lastOutDts[streamIndex] + 1; // 强制单调递增 DTS
                if (packet.pts != AV_NOPTS_VALUE && packet.pts < packet.dts) {
                    packet.pts = packet.dts;
                }
            }
            lastOutDts[streamIndex] = packet.dts;
        }

        ret = av_interleaved_write_frame(outFormatCtx, &packet); // 交织写出，维持音视频交错顺序
        if (ret < 0) {
            FFLOGE("write frame err=%d", ret);
            av_packet_unref(&packet);
            break;
        }

        av_packet_unref(&packet); // 归还 packet 内部引用计数缓冲区
    }

    FFLOGI("push finish, ret=%d", ret);
    return ret;
}

/** @brief 写 trailer，关输出 IO，释放输入输出 AVFormatContext */
void FFRtmpPusher::close() {
    FFLOGI("close");
    av_dict_free(&muxerOptions);
    if (outFormatCtx) {
        av_write_trailer(outFormatCtx); // 写索引/尾部（部分格式需要）
        if (!(outFormatCtx->oformat->flags & AVFMT_NOFILE) && outFormatCtx->pb) {
            avio_closep(&outFormatCtx->pb); // 关闭输出 IO
        }
        avformat_free_context(outFormatCtx);
        outFormatCtx = nullptr;
    }
    if (inFormatCtx) {
        avformat_close_input(&inFormatCtx); // 关闭输入并释放
        inFormatCtx = nullptr;
    }
}
