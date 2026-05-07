package com.demo.service.impl;

import cn.hutool.core.util.IdUtil;
import com.demo.domain.dto.http.resonse.CloudVideoItemRow;
import com.demo.domain.dto.http.resonse.CloudVideoListResponse;
import com.demo.domain.dto.http.resonse.VideoHlsToMp4Response;
import com.demo.domain.dto.http.resonse.VideoUploadChunkResponse;
import com.demo.domain.dto.http.resonse.VideoUploadCompleteResponse;
import com.demo.domain.dto.http.resonse.VideoUploadInitResponse;
import com.demo.domain.entity.VideoRecordEntity;
import com.demo.mapper.VideoRecordMapper;
import com.demo.service.VideoMediaService;
import com.minio.domain.bo.BatchUploadResult;
import com.minio.domain.bo.OssBucketFileItemBo;
import com.minio.domain.bo.UploadItemResult;
import com.minio.domain.entity.OssEntity;
import com.minio.service.OssService;
import com.minio.utils.MinioUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoMediaServiceImpl implements VideoMediaService {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "m4v", "mkv", "webm");
    private static final DecimalFormat DURATION_FORMAT = new DecimalFormat("0.0");
    private static final String STATUS_READY = "READY";

    private final OssService ossService;
    private final MinioUtils minioUtils;
    private final VideoRecordMapper videoRecordMapper;

    @Value("${video.ffmpeg-bin:ffmpeg}")
    private String ffmpegBin;

    @Value("${video.ffprobe-bin:ffprobe}")
    private String ffprobeBin;

    @Value("${video.work-dir:./video-work}")
    private String videoWorkDir;

    @Override
    public VideoUploadInitResponse initUpload(Long userId, String bucketName, String fileName, Long fileSize) {
        SessionMeta meta = new SessionMeta();
        meta.sessionId = buildSessionId(userId, bucketName, fileName, fileSize);
        meta.userId = userId;
        meta.bucketName = bucketName;
        meta.fileName = fileName;
        meta.totalBytes = fileSize;
        Path sessionDir = getUploadSessionDir(meta.sessionId);
        Path metaFile = sessionDir.resolve("meta.properties");
        Path partFile = sessionDir.resolve("upload.part");
        try {
            Files.createDirectories(sessionDir);
            if (Files.exists(metaFile)) {
                SessionMeta existing = readSessionMeta(metaFile);
                if (existing != null) {
                    meta = existing;
                }
            } else {
                writeSessionMeta(metaFile, meta);
            }
            if (!Files.exists(partFile)) {
                Files.createFile(partFile);
            }
            long uploaded = Files.size(partFile);
            if (meta.totalBytes != null && uploaded > meta.totalBytes) {
                uploaded = 0L;
                Files.write(partFile, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            }
            VideoUploadInitResponse response = new VideoUploadInitResponse();
            response.setSessionId(meta.sessionId);
            response.setUploadedBytes(uploaded);
            response.setTotalBytes(meta.totalBytes);
            response.setMessage(uploaded > 0 ? "resume" : "init");
            return response;
        } catch (IOException e) {
            throw new UncheckedIOException("init upload failed", e);
        }
    }

    @Override
    public VideoUploadChunkResponse uploadChunk(String sessionId, Long offset, MultipartFile chunkFile) {
        Path sessionDir = getUploadSessionDir(sessionId);
        Path metaFile = sessionDir.resolve("meta.properties");
        Path partFile = sessionDir.resolve("upload.part");
        SessionMeta meta = readSessionMeta(metaFile);
        if (meta == null) {
            throw new IllegalStateException("upload session not found");
        }
        if (chunkFile == null || chunkFile.isEmpty()) {
            throw new IllegalArgumentException("chunk file empty");
        }
        long safeOffset = offset == null ? 0L : offset;
        try {
            if (!Files.exists(partFile)) {
                Files.createDirectories(sessionDir);
                Files.createFile(partFile);
            }
            long current = Files.size(partFile);
            if (safeOffset != current) {
                throw new IllegalStateException("offset mismatch, client=" + safeOffset + ", server=" + current);
            }
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(partFile.toFile(), "rw");
                 InputStream in = chunkFile.getInputStream()) {
                randomAccessFile.seek(current);
                byte[] buffer = new byte[256 * 1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    randomAccessFile.write(buffer, 0, len);
                }
            }
            long uploaded = Files.size(partFile);
            VideoUploadChunkResponse response = new VideoUploadChunkResponse();
            response.setSessionId(sessionId);
            response.setUploadedBytes(uploaded);
            response.setTotalBytes(meta.totalBytes);
            response.setCompleted(uploaded >= meta.totalBytes);
            return response;
        } catch (IOException e) {
            throw new UncheckedIOException("upload chunk failed", e);
        }
    }

    @Override
    public VideoUploadCompleteResponse completeUpload(String sessionId) {
        Path sessionDir = getUploadSessionDir(sessionId);
        Path metaFile = sessionDir.resolve("meta.properties");
        Path partFile = sessionDir.resolve("upload.part");
        SessionMeta meta = readSessionMeta(metaFile);
        if (meta == null) {
            throw new IllegalStateException("upload session not found");
        }
        Long uploadedFileId = null;
        String uploadedCoverObject = null;
        boolean duplicatedUpload = false;
        try {
            if (!Files.exists(partFile)) {
                throw new IllegalStateException("part file not found");
            }
            long uploaded = Files.size(partFile);
            if (uploaded < meta.totalBytes) {
                throw new IllegalStateException("upload not finished");
            }
            Path completedFile = sessionDir.resolve(sanitizeFileName(meta.fileName));
            Files.copy(partFile, completedFile, StandardCopyOption.REPLACE_EXISTING);

            BatchUploadResult uploadResult = ossService.uploadLocalFiles(
                    List.of(completedFile.toFile()),
                    meta.userId,
                    meta.bucketName
            );
            UploadItemResult item = uploadResult.getItems()
                    .stream()
                    .filter(UploadItemResult::isSuccess)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("minio upload failed"));
            duplicatedUpload = item.isDuplicated();

            uploadedFileId = item.getFileId();
            if (uploadedFileId == null) {
                throw new IllegalStateException("uploaded fileId is empty");
            }
            OssEntity source = ossService.getFileInfoByFileId(uploadedFileId);
            if (source == null) {
                throw new IllegalStateException("uploaded source file not found");
            }
            VideoMeta metaInfo = extractVideoMeta(completedFile);
            uploadedCoverObject = uploadCoverAndRecord(source, completedFile, metaInfo);

            VideoUploadCompleteResponse response = new VideoUploadCompleteResponse();
            response.setSessionId(sessionId);
            response.setFileId(String.valueOf(uploadedFileId));
            response.setUrl(item.getUrl());
            response.setMessage(item.isDuplicated() ? "duplicate-hit" : "ok");
            return response;
        } catch (Exception e) {
            if (uploadedFileId != null && !duplicatedUpload) {
                try {
                    ossService.deleteFileByFileId(uploadedFileId);
                } catch (Exception ignored) {
                }
            }
            if (uploadedCoverObject != null && !duplicatedUpload) {
                try {
                    minioUtils.removeFile(meta.bucketName, uploadedCoverObject);
                } catch (Exception ignored) {
                }
            }
            if (e instanceof IOException ioException) {
                throw new UncheckedIOException("complete upload failed", ioException);
            }
            throw new IllegalStateException("视频封面/元数据提取失败，上传已回滚: " + e.getMessage(), e);
        } finally {
            cleanupSessionDir(sessionDir);
        }
    }

    @Override
    public CloudVideoListResponse listCloudVideos(Long userId, String bucketName, String baseUrl) {
        CloudVideoListResponse response = new CloudVideoListResponse();
        response.setUserId(String.valueOf(userId));
        response.setBucketName(bucketName);
        List<CloudVideoItemRow> items = new ArrayList<>();
        List<OssBucketFileItemBo> rows = ossService.listFileItemsByUserIdAndBucket(userId, bucketName);
        for (OssBucketFileItemBo row : rows) {
            if (row == null || row.getFileId() == null) {
                continue;
            }
            OssEntity source = ossService.getFileInfoByFileId(row.getFileId());
            if (source == null || !isVideoFile(source.getOriginFileName())) {
                continue;
            }
            VideoRecordEntity record = videoRecordMapper.selectByFileId(row.getFileId());
            if (record == null || !isRecordReadyForList(record)) {
                tryBackfillVideoRecord(source);
                record = videoRecordMapper.selectByFileId(row.getFileId());
            }
            if (record == null || !isRecordReadyForList(record)) {
                continue;
            }
            CloudVideoItemRow item = new CloudVideoItemRow();
            item.setFileId(String.valueOf(row.getFileId()));
            String fileName = StringUtils.hasText(record.getVideoName()) ? record.getVideoName() : source.getOriginFileName();
            item.setFileName(fileName);
            long fileSizeBytes = record.getFileSizeBytes() == null ? source.getFileSize() : record.getFileSizeBytes();
            item.setFileSize(String.valueOf(fileSizeBytes));
            double durationSec = record.getDurationSec() == null ? 0D : record.getDurationSec();
            item.setDurationSec(DURATION_FORMAT.format(durationSec));
            long bitrateKbps = record.getBitrateKbps() == null ? 0L : record.getBitrateKbps();
            item.setBitrateKbps(String.valueOf(bitrateKbps));
            String coverUrl = "";
            if (StringUtils.hasText(record.getCoverObjectName())) {
                coverUrl = getSafeUrl(source.getBucketName(), record.getCoverObjectName());
            }
            item.setCoverUrl(coverUrl);
            item.setHlsUrl(baseUrl + "/video/cloud/hls/play.m3u8?fileId=" + row.getFileId());
            item.setMp4DownloadUrl(baseUrl + "/video/cloud/download/mp4?fileId=" + row.getFileId());
            items.add(item);
        }
        response.setItems(items);
        return response;
    }

    @Override
    public String buildPlayM3u8(Long fileId, String baseUrl) {
        OssEntity source = getSourceOss(fileId);
        ensureVideoArtifacts(source);
        String hlsIndexObject = buildHlsIndexObjectName(source.getObjectName());
        try (InputStream inputStream = minioUtils.getObject(source.getBucketName(), hlsIndexObject);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String prefix = baseUrl + "/video/cloud/hls/segment?fileId=" + fileId + "&segment=";
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!StringUtils.hasText(trimmed) || trimmed.startsWith("#")) {
                    lines.add(line);
                } else {
                    String encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8);
                    lines.add(prefix + encoded);
                }
            }
            return String.join("\n", lines) + "\n";
        } catch (IOException e) {
            throw new UncheckedIOException("build m3u8 failed", e);
        } catch (Exception e) {
            throw new IllegalStateException("read m3u8 from minio failed", e);
        }
    }

    @Override
    public InputStream getHlsSegmentStream(Long fileId, String segmentName) {
        if (!StringUtils.hasText(segmentName) || segmentName.contains("..") || segmentName.contains("\\")) {
            throw new IllegalArgumentException("invalid segment name");
        }
        OssEntity source = getSourceOss(fileId);
        ensureVideoArtifacts(source);
        String objectName = buildHlsSegmentObjectName(source.getObjectName(), segmentName);
        try {
            return minioUtils.getObject(source.getBucketName(), objectName);
        } catch (Exception e) {
            throw new IllegalStateException("read hls segment failed", e);
        }
    }

    @Override
    public InputStream getThumbnailStream(Long fileId) {
        OssEntity source = getSourceOss(fileId);
        ensureVideoArtifacts(source);
        String thumbObject = buildThumbnailObjectName(source.getObjectName());
        try {
            return minioUtils.getObject(source.getBucketName(), thumbObject);
        } catch (Exception e) {
            throw new IllegalStateException("read thumbnail failed", e);
        }
    }

    @Override
    public VideoSource getSourceVideo(Long fileId) {
        OssEntity source = getSourceOss(fileId);
        try {
            InputStream inputStream = minioUtils.getObject(source.getBucketName(), source.getObjectName());
            return new VideoSource(
                    source.getOriginFileName(),
                    StringUtils.hasText(source.getContentType()) ? source.getContentType() : "application/octet-stream",
                    inputStream
            );
        } catch (Exception e) {
            throw new IllegalStateException("read source video failed", e);
        }
    }

    @Override
    public VideoSource getHlsMp4Video(Long fileId) {
        OssEntity source = getSourceOss(fileId);
        String targetObject = buildHlsMp4ObjectName(source.getObjectName());
        try {
            if (!minioUtils.isObjectExist(source.getBucketName(), targetObject)) {
                throw new IllegalStateException("hls mp4 not found, please convert first");
            }
            InputStream inputStream = minioUtils.getObject(source.getBucketName(), targetObject);
            return new VideoSource(
                    removeVideoExtension(source.getOriginFileName()) + "_hls.mp4",
                    "video/mp4",
                    inputStream
            );
        } catch (Exception e) {
            throw new IllegalStateException("read hls mp4 failed", e);
        }
    }

    @Override
    public VideoHlsToMp4Response convertHlsToMp4(Long fileId, String baseUrl) {
        OssEntity source = getSourceOss(fileId);
        ensureVideoArtifacts(source);
        String targetObject = buildHlsMp4ObjectName(source.getObjectName());
        boolean alreadyExists = minioUtils.isObjectExist(source.getBucketName(), targetObject);
        if (!alreadyExists) {
            Path hlsDir = getHlsDir(fileId);
            Path localM3u8 = hlsDir.resolve("index.m3u8");
            Path outputMp4 = getFileWorkDir(fileId).resolve("hls_merged.mp4");
            runCommand(List.of(
                    ffmpegBin,
                    "-y",
                    "-allowed_extensions",
                    "ALL",
                    "-i",
                    localM3u8.toString(),
                    "-c",
                    "copy",
                    outputMp4.toString()
            ), getFileWorkDir(fileId));
            try {
                minioUtils.uploadLocalFile(source.getBucketName(), targetObject, outputMp4.toString());
            } catch (Exception e) {
                throw new IllegalStateException("upload hls mp4 failed", e);
            }
        }
        VideoHlsToMp4Response response = new VideoHlsToMp4Response();
        response.setFileId(String.valueOf(fileId));
        response.setDownloadUrl(baseUrl + "/video/cloud/download/hls-mp4?fileId=" + fileId);
        response.setMessage(alreadyExists ? "exists" : "ok");
        return response;
    }

    private String uploadCoverAndRecord(OssEntity source, Path localVideoPath, VideoMeta metaInfo) throws Exception {
        Path fileWorkDir = getFileWorkDir(source.getId());
        Files.createDirectories(fileWorkDir);
        Path coverPath = fileWorkDir.resolve("thumb_upload.jpg");
        generateCover(localVideoPath, coverPath, fileWorkDir);
        String coverObjectName = buildThumbnailObjectName(source.getObjectName());
        minioUtils.uploadLocalFile(source.getBucketName(), coverObjectName, coverPath.toString());
        upsertVideoRecord(
                source,
                coverObjectName,
                null,
                metaInfo.durationSec,
                metaInfo.bitrateKbps,
                STATUS_READY,
                null
        );
        return coverObjectName;
    }

    private void tryBackfillVideoRecord(OssEntity source) {
        try {
            Path fileWorkDir = getFileWorkDir(source.getId());
            Files.createDirectories(fileWorkDir);
            Path sourcePath = getSourceLocalPath(source.getId(), source.getOriginFileName());
            if (!Files.exists(sourcePath)) {
                try (InputStream inputStream = minioUtils.getObject(source.getBucketName(), source.getObjectName())) {
                    Files.copy(inputStream, sourcePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            VideoMeta metaInfo = extractVideoMeta(sourcePath);
            uploadCoverAndRecord(source, sourcePath, metaInfo);
        } catch (Exception e) {
            log.warn("[video] backfill video record failed, fileId={}", source.getId(), e);
        }
    }

    private boolean isRecordReadyForList(VideoRecordEntity record) {
        if (record == null) {
            return false;
        }
        return STATUS_READY.equalsIgnoreCase(record.getStatus())
                && StringUtils.hasText(record.getCoverObjectName())
                && record.getDurationSec() != null
                && record.getBitrateKbps() != null;
    }

    private VideoMeta extractVideoMeta(Path sourcePath) {
        String output = runCommand(List.of(
                ffprobeBin,
                "-v",
                "error",
                "-show_entries",
                "format=duration,bit_rate",
                "-of",
                "default=noprint_wrappers=1:nokey=1",
                sourcePath.toString()
        ), sourcePath.getParent());
        List<String> lines = output.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        double duration = 0D;
        long bitrate = 0L;
        if (!lines.isEmpty()) {
            duration = Double.parseDouble(lines.get(0));
        }
        if (lines.size() > 1) {
            long bitRatePerSec = Long.parseLong(lines.get(1));
            bitrate = Math.max(0L, bitRatePerSec / 1000L);
        }
        return new VideoMeta(duration, bitrate);
    }

    private void generateCover(Path sourcePath, Path coverPath, Path workDir) {
        try {
            runCommand(List.of(
                    ffmpegBin,
                    "-y",
                    "-ss",
                    "00:00:01",
                    "-i",
                    sourcePath.toString(),
                    "-frames:v",
                    "1",
                    coverPath.toString()
            ), workDir);
        } catch (Exception first) {
            runCommand(List.of(
                    ffmpegBin,
                    "-y",
                    "-ss",
                    "00:00:00",
                    "-i",
                    sourcePath.toString(),
                    "-frames:v",
                    "1",
                    coverPath.toString()
            ), workDir);
        }
        if (!Files.exists(coverPath)) {
            throw new IllegalStateException("cover file not generated");
        }
    }

    private OssEntity getSourceOss(Long fileId) {
        OssEntity source = ossService.getFileInfoByFileId(fileId);
        if (source == null) {
            throw new IllegalArgumentException("file not found");
        }
        if (!isVideoFile(source.getOriginFileName())) {
            throw new IllegalArgumentException("file is not video");
        }
        return source;
    }

    private void ensureVideoArtifacts(OssEntity source) {
        Path fileWorkDir = getFileWorkDir(source.getId());
        Path sourcePath = getSourceLocalPath(source.getId(), source.getOriginFileName());
        try {
            Files.createDirectories(fileWorkDir);
            if (!Files.exists(sourcePath)) {
                try (InputStream inputStream = minioUtils.getObject(source.getBucketName(), source.getObjectName())) {
                    Files.copy(inputStream, sourcePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            String thumbnailObject = buildThumbnailObjectName(source.getObjectName());
            if (!minioUtils.isObjectExist(source.getBucketName(), thumbnailObject)) {
                Path thumbnailPath = fileWorkDir.resolve("thumb.jpg");
                generateCover(sourcePath, thumbnailPath, fileWorkDir);
                minioUtils.uploadLocalFile(source.getBucketName(), thumbnailObject, thumbnailPath.toString());
            }

            String hlsIndexObject = buildHlsIndexObjectName(source.getObjectName());
            Path hlsDir = getHlsDir(source.getId());
            Path localHlsIndex = hlsDir.resolve("index.m3u8");
            if (!Files.exists(localHlsIndex)) {
                if (!minioUtils.isObjectExist(source.getBucketName(), hlsIndexObject)) {
                    Files.createDirectories(hlsDir);
                    Path segmentPattern = hlsDir.resolve("seg_%05d.ts");
                    runCommand(List.of(
                            ffmpegBin,
                            "-y",
                            "-i",
                            sourcePath.toString(),
                            "-c:v",
                            "libx264",
                            "-c:a",
                            "aac",
                            "-hls_time",
                            "6",
                            "-hls_list_size",
                            "0",
                            "-hls_segment_filename",
                            segmentPattern.toString(),
                            localHlsIndex.toString()
                    ), fileWorkDir);
                    try (Stream<Path> stream = Files.list(hlsDir)) {
                        for (Path path : stream.toList()) {
                            String objectName = buildHlsSegmentObjectName(source.getObjectName(), path.getFileName().toString());
                            minioUtils.uploadLocalFile(source.getBucketName(), objectName, path.toString());
                        }
                    }
                } else {
                    downloadHlsCacheFromMinio(source, hlsDir, localHlsIndex);
                }
            }
            upsertVideoRecord(source, thumbnailObject, hlsIndexObject, null, null, STATUS_READY, null);
        } catch (Exception e) {
            throw new IllegalStateException("ensure video artifacts failed", e);
        }
    }

    private void downloadHlsCacheFromMinio(OssEntity source, Path hlsDir, Path localHlsIndex) throws Exception {
        Files.createDirectories(hlsDir);
        try (InputStream indexInput = minioUtils.getObject(source.getBucketName(), buildHlsIndexObjectName(source.getObjectName()))) {
            Files.copy(indexInput, localHlsIndex, StandardCopyOption.REPLACE_EXISTING);
        }
        List<String> segments = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(localHlsIndex), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (StringUtils.hasText(trimmed) && !trimmed.startsWith("#")) {
                    segments.add(trimmed);
                }
            }
        }
        for (String segment : segments) {
            Path localSegment = hlsDir.resolve(segment);
            try (InputStream segmentInput = minioUtils.getObject(
                    source.getBucketName(),
                    buildHlsSegmentObjectName(source.getObjectName(), segment)
            )) {
                Files.copy(segmentInput, localSegment, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void upsertVideoRecord(
            OssEntity source,
            String coverObjectName,
            String hlsIndexObject,
            Double durationSec,
            Long bitrateKbps,
            String status,
            String errorMessage
    ) {
        long now = System.currentTimeMillis();
        VideoRecordEntity record = videoRecordMapper.selectByFileId(source.getId());
        String finalVideoName = source.getOriginFileName();
        Long finalFileSizeBytes = source.getFileSize();
        Double finalDurationSec = durationSec;
        Long finalBitrateKbps = bitrateKbps;
        String finalCoverObject = coverObjectName;
        String finalHlsObject = hlsIndexObject;
        String finalStatus = StringUtils.hasText(status) ? status : STATUS_READY;
        String finalErrorMessage = errorMessage;
        if (record != null) {
            if (!StringUtils.hasText(finalVideoName)) {
                finalVideoName = record.getVideoName();
            }
            if (finalFileSizeBytes == null) {
                finalFileSizeBytes = record.getFileSizeBytes();
            }
            if (finalDurationSec == null) {
                finalDurationSec = record.getDurationSec();
            }
            if (finalBitrateKbps == null) {
                finalBitrateKbps = record.getBitrateKbps();
            }
            if (!StringUtils.hasText(finalCoverObject)) {
                finalCoverObject = record.getCoverObjectName();
            }
            if (!StringUtils.hasText(finalHlsObject)) {
                finalHlsObject = record.getHlsObjectName();
            }
            if (!StringUtils.hasText(finalStatus)) {
                finalStatus = record.getStatus();
            }
            if (finalErrorMessage == null) {
                finalErrorMessage = record.getErrorMessage();
            }
        }
        if (record == null) {
            VideoRecordEntity newRecord = new VideoRecordEntity();
            newRecord.setId(IdUtil.getSnowflakeNextId());
            newRecord.setUserId(source.getUserId());
            newRecord.setFileId(source.getId());
            newRecord.setVideoName(finalVideoName);
            newRecord.setFileSizeBytes(finalFileSizeBytes);
            newRecord.setDurationSec(finalDurationSec);
            newRecord.setBitrateKbps(finalBitrateKbps);
            newRecord.setObjectName(source.getObjectName());
            newRecord.setCoverObjectName(finalCoverObject);
            newRecord.setHlsObjectName(finalHlsObject);
            newRecord.setStatus(finalStatus);
            newRecord.setErrorMessage(finalErrorMessage);
            newRecord.setCreatedAt(now);
            newRecord.setUpdatedAt(now);
            videoRecordMapper.insert(newRecord);
            return;
        }
        videoRecordMapper.updateByFileId(
                source.getId(),
                finalVideoName,
                finalFileSizeBytes,
                finalDurationSec,
                finalBitrateKbps,
                source.getObjectName(),
                finalCoverObject,
                finalHlsObject,
                finalStatus,
                finalErrorMessage,
                now
        );
    }

    private double probeDurationSafe(Path sourcePath) {
        if (!Files.exists(sourcePath)) {
            return 0D;
        }
        try {
            String output = runCommand(List.of(
                    ffprobeBin,
                    "-v",
                    "error",
                    "-show_entries",
                    "format=duration",
                    "-of",
                    "default=noprint_wrappers=1:nokey=1",
                    sourcePath.toString()
            ), sourcePath.getParent());
            String value = output.lines().findFirst().orElse("0").trim();
            return Double.parseDouble(value);
        } catch (Exception e) {
            log.warn("[video] probe duration failed: {}", sourcePath, e);
            return 0D;
        }
    }

    private String runCommand(List<String> command, Path workingDir) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (workingDir != null) {
            processBuilder.directory(workingDir.toFile());
        }
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output;
            try (InputStream inputStream = process.getInputStream()) {
                output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            int code = process.waitFor();
            if (code != 0) {
                throw new IllegalStateException("command failed(" + code + "): " + String.join(" ", command) + "\n" + output);
            }
            return output;
        } catch (IOException e) {
            throw new UncheckedIOException("command io failed: " + String.join(" ", command), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("command interrupted", e);
        }
    }

    private Path getWorkRoot() {
        return Path.of(videoWorkDir).toAbsolutePath().normalize();
    }

    private Path getUploadRoot() {
        return getWorkRoot().resolve("upload");
    }

    private Path getUploadSessionDir(String sessionId) {
        return getUploadRoot().resolve(sessionId);
    }

    private Path getFileWorkDir(Long fileId) {
        return getWorkRoot().resolve("cloud").resolve(String.valueOf(fileId));
    }

    private Path getHlsDir(Long fileId) {
        return getFileWorkDir(fileId).resolve("hls");
    }

    private Path getSourceLocalPath(Long fileId, String sourceName) {
        String ext = getExtension(sourceName);
        if (!StringUtils.hasText(ext)) {
            ext = "mp4";
        }
        return getFileWorkDir(fileId).resolve("source." + ext);
    }

    private SessionMeta readSessionMeta(Path metaFile) {
        try {
            if (!Files.exists(metaFile)) {
                return null;
            }
            Properties properties = new Properties();
            try (InputStream in = Files.newInputStream(metaFile)) {
                properties.load(in);
            }
            SessionMeta meta = new SessionMeta();
            meta.sessionId = properties.getProperty("sessionId");
            meta.userId = Long.parseLong(properties.getProperty("userId"));
            meta.bucketName = properties.getProperty("bucketName");
            meta.fileName = properties.getProperty("fileName");
            meta.totalBytes = Long.parseLong(properties.getProperty("totalBytes"));
            return meta;
        } catch (Exception e) {
            throw new IllegalStateException("read session meta failed", e);
        }
    }

    private void writeSessionMeta(Path metaFile, SessionMeta meta) {
        Properties properties = new Properties();
        properties.setProperty("sessionId", meta.sessionId);
        properties.setProperty("userId", String.valueOf(meta.userId));
        properties.setProperty("bucketName", meta.bucketName);
        properties.setProperty("fileName", meta.fileName);
        properties.setProperty("totalBytes", String.valueOf(meta.totalBytes));
        try {
            Files.createDirectories(metaFile.getParent());
            try (var out = Files.newOutputStream(metaFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(out, "video upload session");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("write session meta failed", e);
        }
    }

    private void cleanupSessionDir(Path sessionDir) {
        if (sessionDir == null || !Files.exists(sessionDir)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(sessionDir)) {
            List<Path> all = stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).collect(Collectors.toList());
            for (Path path : all) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("[video] cleanup session failed: {}", sessionDir, e);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "video.mp4";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String buildSessionId(Long userId, String bucketName, String fileName, Long fileSize) {
        String raw = userId + "|" + bucketName + "|" + fileName + "|" + fileSize;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return String.valueOf(raw.hashCode());
        }
    }

    private boolean isVideoFile(String fileName) {
        String ext = getExtension(fileName).toLowerCase(Locale.ROOT);
        return VIDEO_EXTENSIONS.contains(ext);
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index >= fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1);
    }

    private String removeVideoExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "video";
        }
        int index = fileName.lastIndexOf('.');
        if (index <= 0) {
            return fileName;
        }
        return fileName.substring(0, index);
    }

    private String buildThumbnailObjectName(String sourceObjectName) {
        return sourceObjectName + ".thumb.jpg";
    }

    private String buildHlsPrefix(String sourceObjectName) {
        return sourceObjectName + ".hls/";
    }

    private String buildHlsIndexObjectName(String sourceObjectName) {
        return buildHlsPrefix(sourceObjectName) + "index.m3u8";
    }

    private String buildHlsSegmentObjectName(String sourceObjectName, String segmentName) {
        return buildHlsPrefix(sourceObjectName) + segmentName;
    }

    private String buildHlsMp4ObjectName(String sourceObjectName) {
        return sourceObjectName + ".hls.mp4";
    }

    private String getSafeUrl(String bucketName, String objectName) {
        try {
            return minioUtils.getPresignedObjectUrl(bucketName, objectName);
        } catch (Exception e) {
            log.warn("[video] get cover url failed, bucket={}, object={}", bucketName, objectName, e);
            return "";
        }
    }

    private static class VideoMeta {
        private final double durationSec;
        private final long bitrateKbps;

        private VideoMeta(double durationSec, long bitrateKbps) {
            this.durationSec = durationSec;
            this.bitrateKbps = bitrateKbps;
        }
    }

    private static class SessionMeta {
        private String sessionId;
        private Long userId;
        private String bucketName;
        private String fileName;
        private Long totalBytes;
    }
}
