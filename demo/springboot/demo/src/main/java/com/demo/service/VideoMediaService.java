package com.demo.service;

import com.demo.domain.dto.http.resonse.CloudVideoListResponse;
import com.demo.domain.dto.http.resonse.VideoHlsToMp4Response;
import com.demo.domain.dto.http.resonse.VideoUploadChunkResponse;
import com.demo.domain.dto.http.resonse.VideoUploadCompleteResponse;
import com.demo.domain.dto.http.resonse.VideoUploadInitResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface VideoMediaService {
    VideoUploadInitResponse initUpload(Long userId, String bucketName, String fileName, Long fileSize);

    VideoUploadChunkResponse uploadChunk(String sessionId, Long offset, MultipartFile chunkFile);

    VideoUploadCompleteResponse completeUpload(String sessionId);

    CloudVideoListResponse listCloudVideos(Long userId, String bucketName, String baseUrl);

    String buildPlayM3u8(Long fileId, String baseUrl);

    InputStream getHlsSegmentStream(Long fileId, String segmentName);

    InputStream getThumbnailStream(Long fileId);

    VideoSource getSourceVideo(Long fileId);

    VideoSource getHlsMp4Video(Long fileId);

    VideoHlsToMp4Response convertHlsToMp4(Long fileId, String baseUrl);

    class VideoSource {
        private final String fileName;
        private final String contentType;
        private final InputStream inputStream;

        public VideoSource(String fileName, String contentType, InputStream inputStream) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.inputStream = inputStream;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }

        public InputStream getInputStream() {
            return inputStream;
        }
    }
}
