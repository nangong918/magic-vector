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

    // 遍历输入文件的所有流（视频流、音频流、字幕流等）
    for (int i = 0; i < inFormatCtx->nb_streams; ++i) {
        // 获取当前输入流
        AVStream *in_stream = inFormatCtx->streams[i];

        // 根据输入流的编码ID查找对应的编码器
        // 注意：纯封装转换(remux)场景下，这里仅需要一个编码器占位，不实际执行编码
        const auto *codec = avcodec_find_encoder(in_stream->codecpar->codec_id);

        // 为输出的媒体文件创建一个新的流，与输入流对应
        AVStream *out_stream = avformat_new_stream(outFormatCtx, codec);

        // 关键：直接拷贝编码参数，不进行重新编码，保证速度最快
        avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);

        // 设置编码标签为0，让输出封装器自动生成合适的格式标识(FourCC)
        out_stream->codecpar->codec_tag = 0;

        // 判断当前流类型：视频流
        if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            // 记录视频流索引，采用覆盖策略：仅保留最后一个视频流（常规视频文件只有一条）
            video_index = i;
        }
        // 判断当前流类型：音频流
        else if (in_stream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            // 只记录第一条音频流索引，忽略多音轨文件中的其他音轨（如解说、伴奏）
            if (audio_index == -1) {
                audio_index = i;
            }
        }
    }

    // 判断输出格式是否需要关联文件
    // 有些格式(如RTSP)是纯网络流，不需要文件IO
    if (!(outFormatCtx->oformat->flags & AVFMT_NOFILE)) {
        // 打开输出文件/网络IO上下文，用于写入媒体数据
        ret = avio_open2(&outFormatCtx->pb, outputPath, AVIO_FLAG_WRITE, nullptr, nullptr);
        if (ret < 0) {
            FFLOGE("avio open error=%d", ret);
            return ret;
        }
    }

    // 判断是否为RTSP推流输出，进行专属配置
    if (is_rtsp_output(outputPath)) {
        // RTSP 推流优先使用TCP传输，相比UDP更稳定，避免弱网丢包导致播放异常
        av_dict_set(&muxerOptions, "rtsp_transport", "tcp", 0);
        // 设置最小推流延迟，平滑RTP数据包发送，减少网络突发卡顿
        av_dict_set(&muxerOptions, "muxdelay", "0.1", 0);
    }

    // 写入媒体文件头信息（包含编码格式、流信息、SDP、FLV Header等）
    ret = avformat_write_header(outFormatCtx, &muxerOptions);
    if (ret < 0) {
        FFLOGE("avformat_write_header err=%d", ret);
    }

    // 释放参数字典，防止内存泄漏
    // avformat_write_header可能已内部清空，此处再次释放保证安全
    av_dict_free(&muxerOptions);
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
 * @brief 循环读取音视频压缩包 AVPacket，根据媒体时间戳进行同步休眠，再交错写入输出（RTMP/RTSP/HLS）
 * @return 成功返回0，失败返回错误码
 */
int FFRtmpPusher::push() {
    // 返回值初始化
    int ret = 0;
    // 记录推流开始的系统时间（微秒），用于控制推流速度，实现实时同步
    int64_t startTime = av_gettime();
    // 保存每个流的第一个PTS，用于将时间戳归零，从0开始
    std::vector<int64_t> firstPts(inFormatCtx->nb_streams, AV_NOPTS_VALUE);
    // 保存每个流的第一个DTS，用于将时间戳归零
    std::vector<int64_t> firstDts(inFormatCtx->nb_streams, AV_NOPTS_VALUE);
    // 保存上一次输出的DTS，用于保证DTS单调递增，防止推流出错
    std::vector<int64_t> lastOutDts(inFormatCtx->nb_streams, AV_NOPTS_VALUE);
    FFLOGI("push start");

    // 循环读取音视频数据包，直到文件结束或出错
    while (true) {
        // 从输入文件/流中读取一个音视频压缩包
        ret = av_read_frame(inFormatCtx, &packet);
        // 读取失败
        if (ret < 0) {
            // 文件读取完毕，正常结束
            if (ret == AVERROR_EOF) {
                FFLOGI("av_read_frame EOF, treat as normal finish");
                ret = 0;
            } else {
                // 读取发生错误
                FFLOGE("av_read_frame err=%d", ret);
            }
            break;
        }

        // 过滤：只保留之前选定的视频轨和音频轨，其他流（字幕等）直接丢弃
        if (packet.stream_index != video_index && packet.stream_index != audio_index) {
            // 释放数据包资源
            av_packet_unref(&packet);
            continue;
        }

        // 当前数据包所属的流索引
        int streamIndex = packet.stream_index;
        // 获取该流的时间基，用于时间戳换算
        AVRational time_base = inFormatCtx->streams[packet.stream_index]->time_base;

        // ===================== 时间戳归一化（从0开始）=====================
        // 处理PTS（显示时间戳）
        if (packet.pts != AV_NOPTS_VALUE) {
            // 记录第一个PTS，作为基准0点
            if (firstPts[streamIndex] == AV_NOPTS_VALUE) {
                firstPts[streamIndex] = packet.pts;
            }
            // 时间戳减去基准值，实现从0开始
            packet.pts -= firstPts[streamIndex];
            // 防止时间戳为负数
            if (packet.pts < 0) {
                packet.pts = 0;
            }
        }
        // 处理DTS（解码时间戳）
        if (packet.dts != AV_NOPTS_VALUE) {
            // 记录第一个DTS，作为基准0点
            if (firstDts[streamIndex] == AV_NOPTS_VALUE) {
                firstDts[streamIndex] = packet.dts;
            }
            // 时间戳减去基准值
            packet.dts -= firstDts[streamIndex];
            // 防止时间戳为负数
            if (packet.dts < 0) {
                packet.dts = 0;
            }
        }

        // 保证 PTS >= DTS，避免编码器/播放器报错
        if (packet.pts != AV_NOPTS_VALUE
            && packet.dts != AV_NOPTS_VALUE
            && packet.pts < packet.dts) {
            packet.pts = packet.dts;
        }

        // ===================== 实时推流速度控制（同步休眠）=====================
        // 用于同步的时间戳，优先使用DTS
        int64_t syncTs = packet.dts != AV_NOPTS_VALUE ? packet.dts : packet.pts;
        if (syncTs != AV_NOPTS_VALUE) {
            // 将媒体时间戳转换为微秒（真实时间）
            int64_t mediaTimeUs = av_rescale_q(syncTs, time_base, AV_TIME_BASE_Q);
            // 已经过去的系统时间
            int64_t elapsedUs = av_gettime() - startTime;
            // 计算需要休眠的时间，控制推流速度和播放速度一致
            int64_t waitUs = mediaTimeUs - elapsedUs;

            // 需要等待，防止推流过快
            if (waitUs > 0) {
                // 最大休眠200ms，避免连接超时断开
                if (waitUs > 200000) {
                    waitUs = 200000;
                }
                // 休眠等待，实现实时推流
                av_usleep(static_cast<unsigned int>(waitUs));
            }
        }

        // ===================== 时间戳单位转换 =====================
        // 将数据包的时间戳从输入流的时间基，转换为输出流的时间基
        rescale(inFormatCtx, outFormatCtx, &packet);

        // ===================== DTS 单调递增校验 =====================
        if (packet.dts != AV_NOPTS_VALUE) {
            // 如果当前DTS小于等于上一个DTS，强制+1，保证严格递增
            if (lastOutDts[streamIndex] != AV_NOPTS_VALUE
                && packet.dts <= lastOutDts[streamIndex]) {
                packet.dts = lastOutDts[streamIndex] + 1;
                // 同时保证PTS不小于DTS
                if (packet.pts != AV_NOPTS_VALUE && packet.pts < packet.dts) {
                    packet.pts = packet.dts;
                }
            }
            // 更新最后输出的DTS
            lastOutDts[streamIndex] = packet.dts;
        }

        // ===================== 写入输出流 =====================
        // 交错写入音视频包，自动维持音视频顺序，适合推流
        ret = av_interleaved_write_frame(outFormatCtx, &packet);
        if (ret < 0) {
            FFLOGE("write frame err=%d", ret);
            // 释放资源
            av_packet_unref(&packet);
            break;
        }

        // 释放当前数据包，避免内存泄漏
        av_packet_unref(&packet);
    }

    FFLOGI("push finish, ret=%d", ret);
    return ret;
}

/**
 * @brief 写文件尾部信息，关闭输出IO，释放输入/输出上下文
 * 负责推流/封装结束后的资源清理，防止内存泄漏
 */
void FFRtmpPusher::close() {
    // 打印关闭日志
    FFLOGI("close");

    // 释放推流参数字典，防止内存泄漏
    av_dict_free(&muxerOptions);

    // ==================== 释放输出上下文 ====================
    if (outFormatCtx) {
        // 写入文件尾部（如索引、结束标记），MP4/FLV等格式需要
        av_write_trailer(outFormatCtx);

        // 如果输出格式需要文件IO（不是纯网络流），并且IO上下文已打开
        if (!(outFormatCtx->oformat->flags & AVFMT_NOFILE) && outFormatCtx->pb) {
            // 关闭并释放输出文件/网络IO上下文
            avio_closep(&outFormatCtx->pb);
        }

        // 释放输出格式上下文（整个输出的核心结构体）
        avformat_free_context(outFormatCtx);
        // 指针置空，避免野指针
        outFormatCtx = nullptr;
    }

    // ==================== 释放输入上下文 ====================
    if (inFormatCtx) {
        // 关闭输入流，并自动释放输入格式上下文
        avformat_close_input(&inFormatCtx);
        // 指针置空
        inFormatCtx = nullptr;
    }
}
