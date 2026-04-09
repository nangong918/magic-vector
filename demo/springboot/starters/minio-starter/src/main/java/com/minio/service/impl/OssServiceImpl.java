package com.minio.service.impl;

import cn.hutool.core.util.IdUtil;
import com.minio.domain.Do.OssDo;
import com.minio.domain.dto.BatchUploadResult;
import com.minio.domain.dto.UploadItemResult;
import com.minio.mapper.OssMapper;
import com.minio.service.OssService;
import com.minio.utils.MinioUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class OssServiceImpl implements OssService {

    private final OssMapper ossMapper;
    private final MinioUtils minioUtils;

    @Override
    public OssDo getFileInfoByFileId(Long fileId) {
        return ossMapper.getById(fileId);
    }

    @Override
    public BatchUploadResult uploadFiles(List<MultipartFile> files, Long userId, String bucketName) {
        BatchUploadResult result = new BatchUploadResult();
        if (CollectionUtils.isEmpty(files) || userId == null || !StringUtils.hasText(bucketName)) {
            return result;
        }
        for (MultipartFile file : files) {
            UploadItemResult item = new UploadItemResult();
            item.setOriginFileName(file == null ? "" : file.getOriginalFilename());
            if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
                item.setSuccess(false);
                item.setMessage("file is empty");
                result.getItems().add(item);
                result.setFailCount(result.getFailCount() + 1);
                continue;
            }
            try {
                minioUtils.createBucket(bucketName);
                String originFileName = file.getOriginalFilename();
                String idempotentKey = buildIdempotentKey(userId, originFileName, file.getSize());
                OssDo existed = ossMapper.getByIdempotentKey(idempotentKey);
                if (existed != null && existed.getId() != null) {
                    item.setSuccess(true);
                    item.setDuplicated(true);
                    item.setFileId(existed.getId());
                    item.setUrl(getSafeUrl(existed.getBucketName(), existed.getObjectName()));
                    item.setMessage("duplicate-hit");
                    result.getItems().add(item);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                    continue;
                }

                String objectName = buildObjectName(userId, originFileName);
                minioUtils.uploadFile(bucketName, file, objectName, file.getContentType());

                long now = System.currentTimeMillis();
                OssDo ossDo = new OssDo();
                ossDo.setId(IdUtil.getSnowflakeNextId());
                ossDo.setUserId(userId);
                ossDo.setBucketName(bucketName);
                ossDo.setObjectName(objectName);
                ossDo.setOriginFileName(originFileName);
                ossDo.setContentType(file.getContentType());
                ossDo.setFileSize(file.getSize());
                ossDo.setIdempotentKey(idempotentKey);
                ossDo.setCreatedAt(now);
                ossDo.setUpdatedAt(now);
                ossMapper.insert(ossDo);

                item.setSuccess(true);
                item.setFileId(ossDo.getId());
                item.setUrl(getSafeUrl(bucketName, objectName));
                item.setMessage("ok");
                result.getItems().add(item);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                log.warn("[oss] upload failed, file={}", file.getOriginalFilename(), e);
                item.setSuccess(false);
                item.setMessage("upload failed");
                result.getItems().add(item);
                result.setFailCount(result.getFailCount() + 1);
            }
        }
        return result;
    }

    @Override
    public BatchUploadResult uploadLocalFiles(List<File> files, Long userId, String bucketName) {
        BatchUploadResult result = new BatchUploadResult();
        if (CollectionUtils.isEmpty(files) || userId == null || !StringUtils.hasText(bucketName)) {
            return result;
        }
        for (File file : files) {
            UploadItemResult item = new UploadItemResult();
            item.setOriginFileName(file == null ? "" : file.getName());
            if (file == null || !file.exists() || file.length() <= 0) {
                item.setSuccess(false);
                item.setMessage("file not exists");
                result.getItems().add(item);
                result.setFailCount(result.getFailCount() + 1);
                continue;
            }
            try {
                minioUtils.createBucket(bucketName);
                String idempotentKey = buildIdempotentKey(userId, file.getName(), file.length());
                OssDo existed = ossMapper.getByIdempotentKey(idempotentKey);
                if (existed != null && existed.getId() != null) {
                    item.setSuccess(true);
                    item.setDuplicated(true);
                    item.setFileId(existed.getId());
                    item.setUrl(getSafeUrl(existed.getBucketName(), existed.getObjectName()));
                    item.setMessage("duplicate-hit");
                    result.getItems().add(item);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                    continue;
                }

                String objectName = buildObjectName(userId, file.getName());
                minioUtils.uploadLocalFile(bucketName, objectName, file.getAbsolutePath());

                long now = System.currentTimeMillis();
                OssDo ossDo = new OssDo();
                ossDo.setId(IdUtil.getSnowflakeNextId());
                ossDo.setUserId(userId);
                ossDo.setBucketName(bucketName);
                ossDo.setObjectName(objectName);
                ossDo.setOriginFileName(file.getName());
                ossDo.setContentType(null);
                ossDo.setFileSize(file.length());
                ossDo.setIdempotentKey(idempotentKey);
                ossDo.setCreatedAt(now);
                ossDo.setUpdatedAt(now);
                ossMapper.insert(ossDo);

                item.setSuccess(true);
                item.setFileId(ossDo.getId());
                item.setUrl(getSafeUrl(bucketName, objectName));
                item.setMessage("ok");
                result.getItems().add(item);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                log.warn("[oss] upload local file failed, file={}", file.getName(), e);
                item.setSuccess(false);
                item.setMessage("upload failed");
                result.getItems().add(item);
                result.setFailCount(result.getFailCount() + 1);
            }
        }
        return result;
    }

    @Override
    public List<OssDo> listUserFiles(Long userId, String sortBy, Integer offset, Integer size) {
        if (userId == null) {
            return new ArrayList<>();
        }
        int fixedOffset = offset == null || offset < 0 ? 0 : offset;
        int fixedSize = size == null || size <= 0 ? 20 : Math.min(size, 100);
        if ("fileSize".equalsIgnoreCase(sortBy)) {
            return ossMapper.queryByUserIdOrderByFileSize(userId, fixedOffset, fixedSize);
        }
        return ossMapper.queryByUserIdOrderByCreatedAt(userId, fixedOffset, fixedSize);
    }

    @Override
    public @NonNull List<String> getFileUrlsByFileIds(List<Long> fileIds) {
        List<String> urls = new ArrayList<>();
        if (CollectionUtils.isEmpty(fileIds)) {
            return urls;
        }
        for (Long fileId : fileIds) {
            if (fileId == null) {
                urls.add(null);
                continue;
            }
            OssDo ossDo = ossMapper.getById(fileId);
            if (ossDo == null) {
                urls.add(null);
                continue;
            }
            urls.add(getSafeUrl(ossDo.getBucketName(), ossDo.getObjectName()));
        }
        return urls;
    }

    @Override
    public boolean deleteFileByFileId(Long fileId) {
        if (fileId == null) {
            return false;
        }
        OssDo ossDo = ossMapper.getById(fileId);
        if (ossDo == null) {
            return false;
        }
        try {
            minioUtils.removeFile(ossDo.getBucketName(), ossDo.getObjectName());
            return ossMapper.deleteById(fileId) > 0;
        } catch (Exception e) {
            log.warn("[oss] delete failed, fileId={}", fileId, e);
            return false;
        }
    }

    private String buildObjectName(Long userId, String originFileName) {
        return userId + "/" + System.currentTimeMillis() + "_" + IdUtil.getSnowflakeNextId() + "_" + originFileName;
    }

    private String buildIdempotentKey(Long userId, String originFileName, long fileSize) {
        String raw = userId + "_" + originFileName + "_" + fileSize;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return raw;
        }
    }

    private String getSafeUrl(String bucketName, String objectName) {
        try {
            return minioUtils.getPresignedObjectUrl(bucketName, objectName);
        } catch (Exception e) {
            log.warn("[oss] get url failed, bucket={}, object={}", bucketName, objectName, e);
            return "";
        }
    }
}
