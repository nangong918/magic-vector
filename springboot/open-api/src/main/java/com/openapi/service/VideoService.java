package com.openapi.service;

import com.openapi.domain.dto.http.request.VideoUploadCompleteRequest;
import com.openapi.domain.dto.http.request.VideoUploadInitRequest;
import com.openapi.domain.dto.http.request.VideoUploadChunkRequest;
import com.openapi.domain.dto.http.resonse.VideoCloudListResponse;
import com.openapi.domain.dto.http.resonse.VideoDownloadUrlResponse;
import com.openapi.domain.dto.http.resonse.VideoPlayUrlResponse;
import com.openapi.domain.dto.http.resonse.VideoUploadChunkResponse;
import com.openapi.domain.dto.http.resonse.VideoUploadCompleteResponse;
import com.openapi.domain.dto.http.resonse.VideoUploadInitResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {
    VideoUploadInitResponse initUpload(VideoUploadInitRequest request);

    VideoUploadChunkResponse uploadChunk(VideoUploadChunkRequest request, MultipartFile chunkFile);

    VideoUploadCompleteResponse completeUpload(VideoUploadCompleteRequest request);

    VideoCloudListResponse getCloudList(Long userId, Integer page, Integer size);

    VideoPlayUrlResponse getPlayUrl(Long videoId);

    VideoDownloadUrlResponse getDownloadUrl(Long videoId);
}
