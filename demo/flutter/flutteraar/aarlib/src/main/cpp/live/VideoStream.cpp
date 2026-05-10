
#include <cstring>
#include "VideoStream.h"
#include "PushInterface.h"

/**
 * @file VideoStream.cpp（注释合并说明）
 * - 保留原先「第1步 YUV 转换 / 第2步 x264 / 第3步 遍历 NAL」分段注释；
 * - 保留原先 NV21 / I420 每一行拷贝与循环含义注释；
 * - 保留原先 sendSpsPps、sendFrame 与 FLV/AVC 字段说明；
 * - 下列英文 @brief 为补充说明，与中文原注释并存。
 */

VideoStream::VideoStream():m_frameLen(0),
                           videoCodec(nullptr),
                           pic_in(nullptr),
                           videoCallback(nullptr) {
    LOGI("VideoStream created");
}

int VideoStream::setVideoEncInfo(int width, int height, int fps, int bitrate) {
    LOGI("setVideoEncInfo, width=%d, height=%d, fps=%d, bitrate=%d", width, height, fps, bitrate);
    // m_mutex：encodeVideo 可能与其它线程交错调用 setVideoEncInfo；同一时刻只允许一人改 x264 状态或编一帧。
    std::lock_guard<std::mutex> l(m_mutex);
    m_frameLen = width * height;
    if (videoCodec) {
        x264_encoder_close(videoCodec);
        videoCodec = nullptr;
    }
    if (pic_in) {
        x264_picture_clean(pic_in);
        delete pic_in;
        pic_in = nullptr;
    }

    x264_param_t param;
    // ultrafast + zerolatency：直播常用；牺牲压缩率换低延迟、少缓冲。
    int ret = x264_param_default_preset(&param, "ultrafast", "zerolatency");
    if (ret < 0) {
        return ret;
    }
    param.i_level_idc = 32;              // H.264 Level：与分辨率/码率组合需可解码（32 覆盖常见 720p 直播）
    param.i_csp = X264_CSP_I420;         // 输入色彩空间：与 pic_in 平面布局一致
    param.i_width = width;
    param.i_height = height;
    param.i_bframe = 0;                  // 直播关闭 B 帧：降低延迟、简化时间戳
    param.rc.i_rc_method = X264_RC_ABR;   // 平均码率模式：贴近目标码率
    // Java 传入 bitrate 多为 bps；x264 此处按 kbps 填平均码率。
    param.rc.i_bitrate = bitrate / 1024;
    param.rc.i_vbv_max_bitrate = bitrate / 1024 * 1.2; // 峰值略大于平均，允许短时突发
    param.rc.i_vbv_buffer_size = bitrate / 1024;       // VBV 缓冲大小（与码控稳定性相关）

    param.i_fps_num = fps;
    param.i_fps_den = 1;
    param.i_timebase_den = param.i_fps_num; // 时间基分母：与帧率分子对齐
    param.i_timebase_num = param.i_fps_den; // 时间基分子
    param.b_vfr_input = 0;               // 输入帧率固定（摄像头恒定 fps）
    param.i_keyint_max = fps * 2;          // 约每 2 秒一个 IDR，兼顾Seek与码率
    param.b_repeat_headers = 1;          // 重复 SPS/PPS 便于中途入场的解码器恢复
    param.i_threads = 1;                 // 单线程编码：降低多线程帧序抖动（直播常见取舍）

    ret = x264_param_apply_profile(&param, "baseline");
    if (ret < 0) {
        return ret;
    }
    videoCodec = x264_encoder_open(&param);
    if (!videoCodec) {
        return -1;
    }
    pic_in = new x264_picture_t();
    x264_picture_alloc(pic_in, X264_CSP_I420, width, height);
    LOGI("setVideoEncInfo success");
    return ret;
}

void VideoStream::setVideoCallback(VideoCallback callback) {
    this->videoCallback = callback;
}

void VideoStream::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
    LOGI("sendSpsPps, sps_len=%d, pps_len=%d", sps_len, pps_len);
    // FLV Video Tag body：VideoTagHeader(5) + AVCDecoderConfigurationRecord；总长按标准公式。
    int bodySize = 13 + sps_len + 3 + pps_len;
    auto *packet = new RTMPPacket();
    RTMPPacket_Alloc(packet, bodySize);
    int i = 0;
    // 原注释保留：高4bit 帧类型=关键帧(1)，低4bit 编码ID=AVC(7) → 0x17；序列头包仍按 FLV 规范写关键帧+AVC
    packet->m_body[i++] = 0x17;
    // 原注释保留：AVCPacketType=0 表示 AVC sequence header（其后为 AVCDecoderConfigurationRecord）
    packet->m_body[i++] = 0x00;
    // 原注释保留：CompositionTime 三字节 + 扩展（此处三字节全 0，表示 Composition Time offset = 0）
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    // ---------- AVCDecoderConfigurationRecord（ISO/IEC 14496-15）----------
    packet->m_body[i++] = 0x01; // configurationVersion
    packet->m_body[i++] = sps[1]; // AVCProfileIndication
    packet->m_body[i++] = sps[2]; // profile_compatibility
    packet->m_body[i++] = sps[3]; // AVCLevelIndication
    packet->m_body[i++] = 0xFF;   // 原注释保留：含 lengthSizeMinusOne=3 → 每个 NAL 长度占 4 字节

    packet->m_body[i++] = 0xE1; // numOfSequenceParameterSets=1
    packet->m_body[i++] = (sps_len >> 8) & 0xFF;
    packet->m_body[i++] = sps_len & 0xFF;
    memcpy(&packet->m_body[i], sps, sps_len);
    i += sps_len;

    packet->m_body[i++] = 0x01; // numOfPictureParameterSets
    packet->m_body[i++] = (pps_len >> 8) & 0xFF;
    packet->m_body[i++] = (pps_len) & 0xFF;
    memcpy(&packet->m_body[i], pps, pps_len);

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize  = bodySize;
    packet->m_nChannel   = 0x10;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;

    videoCallback(packet);
}

void VideoStream::sendFrame(int type, uint8_t *payload, int i_payload) {
    // 原注释保留：Annex-B 起始码可能是 00 00 01（3字节）或 00 00 00 01（4字节），通过 payload[2]==0 区分
    if (payload[2] == 0x00) {
        i_payload -= 4;
        payload += 4;
    } else {
        i_payload -= 3;
        payload += 3;
    }
    int i = 0;
    // VideoTagHeader(1+1+3) + NAL 长度(4) + 裸 NAL 数据
    int bodySize = 9 + i_payload;
    auto *packet = new RTMPPacket();
    RTMPPacket_Alloc(packet, bodySize);

    if (type == NAL_SLICE_IDR) {
        packet->m_body[i++] = 0x17; // 原注释保留：关键帧 + AVC
    } else {
        packet->m_body[i++] = 0x27; // 原注释保留：非关键帧 + AVC
    }
    packet->m_body[i++] = 0x01; // AVCPacketType = NALU
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    // FLV AVC NALU 前 4 字节为大端 NAL 长度（与 AVCDecoderConfigurationRecord 中 lengthSizeMinusOne 一致）
    packet->m_body[i++] = (i_payload >> 24) & 0xFF;
    packet->m_body[i++] = (i_payload >> 16) & 0xFF;
    packet->m_body[i++] = (i_payload >> 8) & 0xFF;
    packet->m_body[i++] = (i_payload) & 0xFF;

    memcpy(&packet->m_body[i], payload, static_cast<size_t>(i_payload));

    packet->m_hasAbsTimestamp = 0;
    packet->m_nBodySize       = bodySize;
    packet->m_packetType      = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nChannel        = 0x10;
    packet->m_headerType      = RTMP_PACKET_SIZE_LARGE;
    videoCallback(packet);
}

/**
 * 视频软编码（x264）并封装为 RTMP 视频包
 *
 * 流程（原注释保留）：
 *   YUV 数据 → 转换格式/排布 → x264 编码 → NAL 单元 → 区分 SPS/PPS/帧数据 → 发送 RTMP 包
 *
 * @param data       摄像头采集的原始 YUV 数据（YUV420 格式）
 * @param camera_type 摄像头数据类型标志
 *                   1 = NV21（Y 平面连续，UV 交错且 V 在前）
 *                   2 = I420 / YV12（Y、U、V 三个平面分别连续存储）
 */
void VideoStream::encodeVideo(int8_t *data, int camera_type) {
    // 与 setVideoEncInfo 互斥：防止分辨率切换与编码并发破坏 x264 状态。
    std::lock_guard<std::mutex> l(m_mutex);
    // pic_in 未分配说明尚未 setVideoEncInfo 成功，无法编码。
    if (!pic_in) return;

    // ========== 第1步：将 Android 摄像头数据转换为 x264 需要的平面格式 ==========
    // pic_in->img.plane[0] → Y 平面（亮度）
    // pic_in->img.plane[1] → U 平面（色度）
    // pic_in->img.plane[2] → V 平面（色度）

    if (camera_type == 1) {
        // --- NV21 格式转换 ---
        // NV21 内存布局：YYYY... + VUVU...（Y 在前，UV 交错且 V 在前）
        // x264 需要：plane[0]=Y, plane[1]=U, plane[2]=V（各自连续）

        memcpy(pic_in->img.plane[0], data, m_frameLen);

        // 从 NV21 的交错 UV 中分离到 I420 三平面（x264：plane[1]=U，plane[2]=V）
        // NV21：UV 从 data+m_frameLen 起，每 2 字节为 V、U（V 在前 U 在后）
        for (int idx = 0; idx < m_frameLen / 4; ++idx) {
            // U：NV21 每对的第二个字节 → I420 的 U 平面 plane[1]
            *(pic_in->img.plane[1] + idx) = *(data + m_frameLen + idx * 2 + 1);
            // V：NV21 每对的第一个字节 → I420 的 V 平面 plane[2]
            *(pic_in->img.plane[2] + idx) = *(data + m_frameLen + idx * 2);
        }
    } else if (camera_type == 2) {
        // --- I420/YV12 格式转换 ---
        // 内存布局：Y 平面 → U 平面 → V 平面（各自连续存储，直接拷贝即可）

        int offset = 0;

        memcpy(pic_in->img.plane[0], data, (size_t) m_frameLen);
        offset += m_frameLen;

        memcpy(pic_in->img.plane[1], data + offset, (size_t) m_frameLen / 4);
        offset += m_frameLen / 4;

        memcpy(pic_in->img.plane[2], data + offset, (size_t) m_frameLen / 4);
    } else {
        // 原注释保留：不支持的 camera_type，直接返回，避免向 x264 喂非法布局。
        return;
    }

    // ========== 第2步：x264 软编码，将 YUV 图像编码为 H.264 NAL 单元 ==========
    x264_nal_t *pp_nal;           // 输出：NAL 单元数组指针
    int pi_nal;                   // 输出：NAL 单元个数
    x264_picture_t pic_out;       // 输出：编码后的重建图像（供 x264 内部参考，本流程未再读取）

    // x264_encoder_encode：消费一帧 pic_in，产出若干 NAL（SPS/PPS/IDR/P 等）；返回值表示是否输出完整帧。
    x264_encoder_encode(
            videoCodec,           // x264 编码器句柄
            &pp_nal,              // 输出：指向 NAL 数组的指针
            &pi_nal,              // 输出：NAL 单元数量
            pic_in,               // 输入：待编码的 YUV 图像（I420 三平面）
            &pic_out);            // 输出：重建帧信息（本工程仅用编码侧 NAL）

    LOGI("encodeVideo, camera_type=%d, pi_nal=%d", camera_type, pi_nal);

    // ========== 第3步：遍历 NAL 单元，区分类型并发送 ==========
    int sps_len = 0, pps_len = 0;
    uint8_t sps[100];
    uint8_t pps[100];

    for (int idx = 0; idx < pi_nal; ++idx) {
        x264_nal_t nal = pp_nal[idx];

        if (nal.i_type == NAL_SPS) {
            // --- SPS（序列参数集）：包含分辨率、Profile 等全局参数 ---
            // x264 输出的 SPS 前4字节是起始码（0x00000001），跳过它
            sps_len = nal.i_payload - 4;
            memcpy(sps, nal.p_payload + 4, static_cast<size_t>(sps_len));
        } else if (nal.i_type == NAL_PPS) {
            // --- PPS（图像参数集）：包含熵编码模式、量化参数等 ---
            // PPS 前4字节同样是起始码，跳过
            pps_len = nal.i_payload - 4;
            memcpy(pps, nal.p_payload + 4, static_cast<size_t>(pps_len));

            // SPS 和 PPS 成对发送，收到 PPS 后将 SPS+PPS 一起封装为 FLV AVC sequence header
            sendSpsPps(sps, pps, sps_len, pps_len);
        } else {
            // --- 视频帧数据（IDR 关键帧 / P 帧等）：sendFrame 内去掉起始码，仅封装裸 NAL 进 FLV ---
            sendFrame(nal.i_type, nal.p_payload, nal.i_payload);
        }
    }
}

VideoStream::~VideoStream() {
    LOGI("VideoStream destroy");
    if (videoCodec) {
        x264_encoder_close(videoCodec);
        videoCodec = nullptr;
    }
    if (pic_in) {
        x264_picture_clean(pic_in);
        delete pic_in;
        pic_in = nullptr;
    }
}
