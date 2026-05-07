package com.demo.controller;

import com.demo.domain.constant.error.CommonExceptions;
import com.demo.domain.dto.BaseResponse;
import com.demo.domain.dto.http.resonse.CloudVideoListResponse;
import com.demo.domain.dto.http.resonse.VideoHlsToMp4Response;
import com.demo.domain.dto.http.resonse.VideoUploadChunkResponse;
import com.demo.domain.dto.http.resonse.VideoUploadCompleteResponse;
import com.demo.domain.dto.http.resonse.VideoUploadInitResponse;
import com.demo.service.VideoMediaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@CrossOrigin(origins = "*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoMediaController {

    private final VideoMediaService videoMediaService;

    @PostMapping("/upload/init")
    public BaseResponse<VideoUploadInitResponse> initUpload(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "bucketName", defaultValue = "global-oss") String bucketName,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileSize") Long fileSize
    ) {
        if (userId == null || !StringUtils.hasText(bucketName) || !StringUtils.hasText(fileName) || fileSize == null || fileSize <= 0) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        return BaseResponse.getResponseEntitySuccess(videoMediaService.initUpload(userId, bucketName.trim(), fileName.trim(), fileSize));
    }

    @PostMapping("/upload/chunk")
    public BaseResponse<VideoUploadChunkResponse> uploadChunk(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("offset") Long offset,
            @RequestParam("chunkFile") MultipartFile chunkFile
    ) {
        if (!StringUtils.hasText(sessionId) || offset == null || offset < 0 || chunkFile == null || chunkFile.isEmpty()) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        return BaseResponse.getResponseEntitySuccess(videoMediaService.uploadChunk(sessionId.trim(), offset, chunkFile));
    }

    @PostMapping("/upload/complete")
    public BaseResponse<VideoUploadCompleteResponse> completeUpload(
            @RequestParam("sessionId") String sessionId
    ) {
        if (!StringUtils.hasText(sessionId)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        try {
            return BaseResponse.getResponseEntitySuccess(videoMediaService.completeUpload(sessionId.trim()));
        } catch (Exception e) {
            return BaseResponse.LogBackError("C_10007", "视频上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/cloud/list")
    public BaseResponse<CloudVideoListResponse> listCloudVideos(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "bucketName", defaultValue = "global-oss") String bucketName,
            HttpServletRequest request
    ) {
        if (userId == null || !StringUtils.hasText(bucketName)) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        String baseUrl = getBaseUrl(request);
        return BaseResponse.getResponseEntitySuccess(videoMediaService.listCloudVideos(userId, bucketName.trim(), baseUrl));
    }

    @GetMapping(value = "/cloud/hls/play.m3u8", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<String> playM3u8(
            @RequestParam("fileId") Long fileId,
            HttpServletRequest request
    ) {
        String baseUrl = getBaseUrl(request);
        String playlist = videoMediaService.buildPlayM3u8(fileId, baseUrl);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(playlist);
    }

    @GetMapping("/cloud/hls/segment")
    public ResponseEntity<InputStreamResource> getHlsSegment(
            @RequestParam("fileId") Long fileId,
            @RequestParam("segment") String segment
    ) {
        InputStreamResource resource = new InputStreamResource(videoMediaService.getHlsSegmentStream(fileId, segment));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp2t"))
                .body(resource);
    }

    @GetMapping("/cloud/thumbnail")
    public ResponseEntity<InputStreamResource> getThumbnail(
            @RequestParam("fileId") Long fileId
    ) {
        InputStreamResource resource = new InputStreamResource(videoMediaService.getThumbnailStream(fileId));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @GetMapping("/cloud/download/mp4")
    public ResponseEntity<InputStreamResource> downloadMp4(
            @RequestParam("fileId") Long fileId
    ) {
        return asAttachment(videoMediaService.getSourceVideo(fileId));
    }

    @GetMapping("/cloud/download/hls-mp4")
    public ResponseEntity<InputStreamResource> downloadHlsMp4(
            @RequestParam("fileId") Long fileId
    ) {
        return asAttachment(videoMediaService.getHlsMp4Video(fileId));
    }

    @PostMapping("/cloud/hls/to-mp4")
    public BaseResponse<VideoHlsToMp4Response> convertHlsToMp4(
            @RequestParam("fileId") Long fileId,
            HttpServletRequest request
    ) {
        if (fileId == null) {
            return BaseResponse.LogBackError(CommonExceptions.PARAM_ERROR);
        }
        String baseUrl = getBaseUrl(request);
        return BaseResponse.getResponseEntitySuccess(videoMediaService.convertHlsToMp4(fileId, baseUrl));
    }

    private ResponseEntity<InputStreamResource> asAttachment(VideoMediaService.VideoSource source) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(source.getContentType());
        } catch (Exception ignored) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(source.getFileName(), StandardCharsets.UTF_8)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new InputStreamResource(source.getInputStream()));
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean omitPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                || ("https".equalsIgnoreCase(scheme) && port == 443);
        return omitPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }
}
