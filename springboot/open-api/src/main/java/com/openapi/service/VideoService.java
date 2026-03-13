package com.openapi.service;

import com.openapi.domain.dto.request.VideoUploadCompleteRequest;
import com.openapi.domain.dto.request.VideoUploadInitRequest;
import com.openapi.domain.dto.request.VideoUploadChunkRequest;
import com.openapi.domain.dto.resonse.VideoCloudListResponse;
import com.openapi.domain.dto.resonse.VideoDownloadUrlResponse;
import com.openapi.domain.dto.resonse.VideoPlayUrlResponse;
import com.openapi.domain.dto.resonse.VideoUploadChunkResponse;
import com.openapi.domain.dto.resonse.VideoUploadCompleteResponse;
import com.openapi.domain.dto.resonse.VideoUploadInitResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {
    VideoUploadInitResponse initUpload(VideoUploadInitRequest request);

    VideoUploadChunkResponse uploadChunk(VideoUploadChunkRequest request, MultipartFile chunkFile);

    VideoUploadCompleteResponse completeUpload(VideoUploadCompleteRequest request);

    VideoCloudListResponse getCloudList(String userId, Integer page, Integer size);

    VideoPlayUrlResponse getPlayUrl(String videoId);

    VideoDownloadUrlResponse getDownloadUrl(String videoId);
}
