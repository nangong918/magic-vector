package com.openapi.service.impl;

import cn.hutool.core.util.IdUtil;
import com.minio.domain.dto.BatchUploadResult;
import com.minio.domain.dto.UploadItemResult;
import com.minio.service.OssService;
import com.minio.utils.MinioUtils;
import com.openapi.component.manager.video.VideoUploadSessionManager;
import com.openapi.domain.Do.VideoRecordDo;
import com.openapi.domain.dto.http.request.VideoUploadChunkRequest;
import com.openapi.domain.dto.http.request.VideoUploadCompleteRequest;
import com.openapi.domain.dto.http.request.VideoUploadInitRequest;
import com.openapi.domain.dto.http.resonse.VideoCloudListResponse;
import com.openapi.domain.dto.http.resonse.VideoDownloadUrlResponse;
import com.openapi.domain.dto.http.resonse.VideoPlayUrlResponse;
import com.openapi.domain.dto.http.resonse.VideoUploadChunkResponse;
import com.openapi.domain.dto.http.resonse.VideoUploadCompleteResponse;
import com.openapi.domain.dto.http.resonse.VideoUploadInitResponse;
import com.openapi.mapper.VideoRecordMapper;
import com.openapi.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    private static final Integer DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;
    private static final String VIDEO_BUCKET = "video-bucket";

    private final VideoUploadSessionManager sessionManager;
    private final VideoRecordMapper videoRecordMapper;
    private final OssService ossService;
    private final MinioUtils minioUtils;

    @Override
    public VideoUploadInitResponse initUpload(VideoUploadInitRequest request) {
        VideoUploadSessionManager.UploadSession session = sessionManager.createSession(
                request.getUserId(),
                request.getFileName(),
                request.getFileSize(),
                DEFAULT_CHUNK_SIZE
        );
        VideoUploadInitResponse response = new VideoUploadInitResponse();
        response.setUploadId(session.getUploadId());
        response.setUploadedOffset(session.getUploadedOffset());
        response.setChunkSize(session.getChunkSize());
        response.setMessage("ok");
        return response;
    }

    @Override
    public VideoUploadChunkResponse uploadChunk(VideoUploadChunkRequest request, MultipartFile chunkFile) {
        VideoUploadChunkResponse response = new VideoUploadChunkResponse();
        response.setUploadId(request.getUploadId());
        VideoUploadSessionManager.UploadSession session = sessionManager.getSession(request.getUploadId());
        if (session == null) {
            response.setAccepted(Boolean.FALSE);
            response.setMessage("upload session not found");
            response.setUploadedOffset(0L);
            return response;
        }
        if (session.getUserId() == null || !session.getUserId().equals(request.getUserId())) {
            response.setAccepted(Boolean.FALSE);
            response.setMessage("session user mismatch");
            response.setUploadedOffset(session.getUploadedOffset());
            return response;
        }
        if (chunkFile == null || chunkFile.isEmpty()) {
            response.setAccepted(Boolean.FALSE);
            response.setMessage("chunk is empty");
            response.setUploadedOffset(session.getUploadedOffset());
            return response;
        }
        try (RandomAccessFile raf = new RandomAccessFile(session.getTempFile(), "rw")) {
            raf.seek(request.getOffset());
            raf.write(chunkFile.getBytes());
            long nextOffset = request.getOffset() + chunkFile.getSize();
            session.setUploadedOffset(nextOffset);
            response.setAccepted(Boolean.TRUE);
            response.setMessage("chunk accepted");
            response.setUploadedOffset(nextOffset);
            return response;
        } catch (Exception e) {
            log.error("[video upload] write chunk error", e);
            response.setAccepted(Boolean.FALSE);
            response.setMessage("write chunk failed");
            response.setUploadedOffset(session.getUploadedOffset());
            return response;
        }
    }

    @Override
    public VideoUploadCompleteResponse completeUpload(VideoUploadCompleteRequest request) {
        VideoUploadCompleteResponse response = new VideoUploadCompleteResponse();
        VideoUploadSessionManager.UploadSession session = sessionManager.getSession(request.getUploadId());
        if (session == null || !session.getUserId().equals(request.getUserId())) {
            response.setMessage("upload session not found");
            return response;
        }
        try {
            File tempFile = session.getTempFile();
            BatchUploadResult uploadResult = ossService.uploadLocalFiles(List.of(tempFile), session.getUserId(), VIDEO_BUCKET);
            UploadItemResult first = uploadResult.getItems() == null || uploadResult.getItems().isEmpty()
                    ? null
                    : uploadResult.getItems().get(0);
            String objectName = first == null || !first.isSuccess()
                    ? ""
                    : resolveObjectName(first.getFileId());

            VideoRecordDo record = new VideoRecordDo();
            record.setId(IdUtil.getSnowflakeNextId());
            record.setUserId(session.getUserId());
            record.setObjectName(objectName);
            record.setHlsObjectName("TODO_HLS_OBJECT_NAME");
            record.setStatus("UPLOADED_TODO_TRANSCODE");
            record.setCreatedAt(System.currentTimeMillis());
            record.setUpdatedAt(System.currentTimeMillis());
            videoRecordMapper.insert(record);

            // TODO-FFMPEG: 使用 FFmpeg 将 MinIO 对象转码为 m3u8 并更新 hlsObjectName/status
            response.setVideoId(record.getId());
            response.setObjectName(record.getObjectName());
            response.setMessage("upload complete, transcode task TODO");
            return response;
        } catch (Exception e) {
            log.error("[video upload] complete error", e);
            response.setMessage("complete upload failed");
            return response;
        } finally {
            File tempFile = session.getTempFile();
            if (tempFile != null && tempFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
            sessionManager.removeSession(request.getUploadId());
        }
    }

    @Override
    public VideoCloudListResponse getCloudList(Long userId, Integer page, Integer size) {
        int fixedPage = page == null || page <= 0 ? 1 : page;
        int fixedSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
        int offset = (fixedPage - 1) * fixedSize;
        List<VideoRecordDo> records = videoRecordMapper.queryByUserId(userId, offset, fixedSize);
        VideoCloudListResponse response = new VideoCloudListResponse();
        if (records == null) {
            return response;
        }
        records.forEach(it -> {
            VideoCloudListResponse.VideoCloudItem item = new VideoCloudListResponse.VideoCloudItem();
            item.setVideoId(it.getId());
            item.setObjectName(it.getObjectName());
            item.setHlsObjectName(it.getHlsObjectName());
            item.setStatus(it.getStatus());
            item.setCreatedAt(it.getCreatedAt());
            response.getVideos().add(item);
        });
        return response;
    }

    @Override
    public VideoPlayUrlResponse getPlayUrl(Long videoId) {
        VideoPlayUrlResponse response = new VideoPlayUrlResponse();
        VideoRecordDo record = videoRecordMapper.getById(videoId);
        if (record == null) {
            response.setStatus("NOT_FOUND");
            response.setPlayUrl("");
            return response;
        }
        response.setVideoId(record.getId());
        response.setStatus(record.getStatus());
        // TODO-FFMPEG-HLS: 这里应返回 m3u8 对象签名URL；当前临时回退到原始对象URL。
        response.setPlayUrl(getSafeUrl(VIDEO_BUCKET, record.getObjectName()));
        return response;
    }

    @Override
    public VideoDownloadUrlResponse getDownloadUrl(Long videoId) {
        VideoDownloadUrlResponse response = new VideoDownloadUrlResponse();
        VideoRecordDo record = videoRecordMapper.getById(videoId);
        if (record == null) {
            response.setDownloadUrl("");
            return response;
        }
        response.setVideoId(record.getId());
        response.setDownloadUrl(getSafeUrl(VIDEO_BUCKET, record.getObjectName()));
        return response;
    }

    private String getSafeUrl(String bucketName, String objectName) {
        if (!StringUtils.hasText(objectName)) {
            return "";
        }
        try {
            return minioUtils.getPresignedObjectUrl(bucketName, objectName);
        } catch (Exception e) {
            log.warn("[video] presigned url failed, object={}", objectName, e);
            return "";
        }
    }

    private String resolveObjectName(Long fileId) {
        if (fileId == null) {
            return "";
        }
        var ossDo = ossService.getFileInfoByFileId(fileId);
        return ossDo == null || ossDo.getObjectName() == null ? "" : ossDo.getObjectName();
    }
}
