package com.openapi.controller;

import com.openapi.domain.constant.error.CommonExceptions;
import com.openapi.domain.dto.BaseResponse;
import com.openapi.domain.dto.request.VideoUploadChunkRequest;
import com.openapi.domain.dto.request.VideoUploadCompleteRequest;
import com.openapi.domain.dto.request.VideoUploadInitRequest;
import com.openapi.domain.dto.resonse.VideoCloudListResponse;
import com.openapi.domain.dto.resonse.VideoDownloadUrlResponse;
import com.openapi.domain.dto.resonse.VideoPlayUrlResponse;
import com.openapi.domain.dto.resonse.VideoUploadChunkResponse;
import com.openapi.domain.dto.resonse.VideoUploadCompleteResponse;
import com.openapi.domain.dto.resonse.VideoUploadInitResponse;
import com.openapi.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoController {
    private final VideoService videoService;

    @PostMapping("/upload/init")
    public BaseResponse<VideoUploadInitResponse> initUpload(
            @Valid @RequestBody VideoUploadInitRequest request
    ) {
        return BaseResponse.getResponseEntitySuccess(videoService.initUpload(request));
    }

    @PostMapping("/upload/chunk")
    public BaseResponse<VideoUploadChunkResponse> uploadChunk(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("userId") String userId,
            @RequestParam("chunkIndex") Integer chunkIndex,
            @RequestParam("offset") Long offset,
            @RequestParam("chunkFile") MultipartFile chunkFile
    ) {
        if (!StringUtils.hasText(uploadId) || !StringUtils.hasText(userId)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        VideoUploadChunkRequest request = new VideoUploadChunkRequest();
        request.setUploadId(uploadId);
        request.setUserId(userId);
        request.setChunkIndex(chunkIndex);
        request.setOffset(offset);
        return BaseResponse.getResponseEntitySuccess(videoService.uploadChunk(request, chunkFile));
    }

    @PostMapping("/upload/complete")
    public BaseResponse<VideoUploadCompleteResponse> completeUpload(
            @Valid @RequestBody VideoUploadCompleteRequest request
    ) {
        return BaseResponse.getResponseEntitySuccess(videoService.completeUpload(request));
    }

    @GetMapping("/cloud/list")
    public BaseResponse<VideoCloudListResponse> getCloudVideoList(
            @RequestParam("userId") String userId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return BaseResponse.getResponseEntitySuccess(videoService.getCloudList(userId, page, size));
    }

    @GetMapping("/cloud/play-url")
    public BaseResponse<VideoPlayUrlResponse> getCloudPlayUrl(
            @RequestParam("videoId") String videoId
    ) {
        return BaseResponse.getResponseEntitySuccess(videoService.getPlayUrl(videoId));
    }

    @GetMapping("/cloud/download-url")
    public BaseResponse<VideoDownloadUrlResponse> getCloudDownloadUrl(
            @RequestParam("videoId") String videoId
    ) {
        return BaseResponse.getResponseEntitySuccess(videoService.getDownloadUrl(videoId));
    }
}
