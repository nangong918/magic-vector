package com.minio.service.impl;

import cn.hutool.core.util.IdUtil;
import io.minio.ObjectWriteResponse;
import com.minio.domain.bo.OssBucketFileItemBo;
import com.minio.domain.entity.OssEntity;
import com.minio.domain.bo.BatchUploadResult;
import com.minio.domain.bo.UploadItemResult;
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
    public OssEntity getFileInfoByFileId(Long fileId) {
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
                OssEntity existed = ossMapper.getByIdempotentKey(idempotentKey);
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
                ObjectWriteResponse uploadResp = minioUtils.uploadFile(bucketName, file, objectName, file.getContentType());
                ensureUploadSuccess(uploadResp, objectName);

                long now = System.currentTimeMillis();
                OssEntity ossEntity = new OssEntity();
                ossEntity.setId(IdUtil.getSnowflakeNextId());
                ossEntity.setUserId(userId);
                ossEntity.setBucketName(bucketName);
                ossEntity.setObjectName(objectName);
                ossEntity.setOriginFileName(originFileName);
                ossEntity.setContentType(file.getContentType());
                ossEntity.setFileSize(file.getSize());
                ossEntity.setIdempotentKey(idempotentKey);
                ossEntity.setCreatedAt(now);
                ossEntity.setUpdatedAt(now);
                ossMapper.insert(ossEntity);

                item.setSuccess(true);
                item.setFileId(ossEntity.getId());
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
                OssEntity existed = ossMapper.getByIdempotentKey(idempotentKey);
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
                ObjectWriteResponse uploadResp = minioUtils.uploadLocalFile(bucketName, objectName, file.getAbsolutePath());
                ensureUploadSuccess(uploadResp, objectName);

                long now = System.currentTimeMillis();
                OssEntity ossEntity = new OssEntity();
                ossEntity.setId(IdUtil.getSnowflakeNextId());
                ossEntity.setUserId(userId);
                ossEntity.setBucketName(bucketName);
                ossEntity.setObjectName(objectName);
                ossEntity.setOriginFileName(file.getName());
                ossEntity.setContentType(null);
                ossEntity.setFileSize(file.length());
                ossEntity.setIdempotentKey(idempotentKey);
                ossEntity.setCreatedAt(now);
                ossEntity.setUpdatedAt(now);
                ossMapper.insert(ossEntity);

                item.setSuccess(true);
                item.setFileId(ossEntity.getId());
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
    public List<OssEntity> listUserFiles(Long userId, String sortBy, Integer offset, Integer size) {
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
            OssEntity ossEntity = ossMapper.getById(fileId);
            if (ossEntity == null) {
                urls.add(null);
                continue;
            }
            urls.add(getSafeUrl(ossEntity.getBucketName(), ossEntity.getObjectName()));
        }
        return urls;
    }

    @Override
    public boolean updateFileNameByFileId(Long fileId, String newFileName) {
        if (fileId == null || !StringUtils.hasText(newFileName)) {
            return false;
        }
        OssEntity ossEntity = ossMapper.getById(fileId);
        if (ossEntity == null) {
            return false;
        }
        return ossMapper.updateOriginFileNameById(fileId, newFileName, System.currentTimeMillis()) > 0;
    }

    @Override
    public UploadItemResult updateFileContentByFileId(Long fileId, MultipartFile file) {
        UploadItemResult item = new UploadItemResult();
        item.setFileId(fileId);
        item.setOriginFileName(file == null ? null : file.getOriginalFilename());
        if (fileId == null || file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
            item.setSuccess(false);
            item.setMessage("file is empty");
            return item;
        }
        OssEntity existed = ossMapper.getById(fileId);
        if (existed == null) {
            item.setSuccess(false);
            item.setMessage("file not found");
            return item;
        }
        try {
            String originFileName = file.getOriginalFilename();
            ObjectWriteResponse uploadResp = minioUtils.uploadFile(
                    existed.getBucketName(),
                    file,
                    existed.getObjectName(),
                    file.getContentType()
            );
            ensureUploadSuccess(uploadResp, existed.getObjectName());
            String idempotentKey = buildIdempotentKey(existed.getUserId(), originFileName, file.getSize());
            ossMapper.updateContentMetaById(
                    fileId,
                    originFileName,
                    file.getContentType(),
                    file.getSize(),
                    idempotentKey,
                    System.currentTimeMillis()
            );
            item.setSuccess(true);
            item.setDuplicated(false);
            item.setUrl(getSafeUrl(existed.getBucketName(), existed.getObjectName()));
            item.setMessage("ok");
            return item;
        } catch (Exception e) {
            log.warn("[oss] update content failed, fileId={}", fileId, e);
            item.setSuccess(false);
            item.setMessage("update failed");
            return item;
        }
    }

    @Override
    public int deleteFilesByFileIds(List<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return 0;
        }
        int successCount = 0;
        for (Long fileId : fileIds) {
            if (deleteFileByFileId(fileId)) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public int deleteAllFilesByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }
        List<OssEntity> files = ossMapper.queryByUserIdAll(userId);
        if (CollectionUtils.isEmpty(files)) {
            return 0;
        }
        int successCount = 0;
        for (OssEntity file : files) {
            if (file != null && deleteFileByFileId(file.getId())) {
                successCount++;
            }
        }
        return successCount;
    }

    @Override
    public int countFilesByUserId(Long userId) {
        if (userId == null) {
            return 0;
        }
        List<OssEntity> files = ossMapper.queryByUserIdAll(userId);
        return files == null ? 0 : files.size();
    }

    @Override
    public List<String> listBucketNamesByUserId(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<String> names = ossMapper.listDistinctBucketNamesByUserId(userId);
        return names == null ? new ArrayList<>() : names;
    }

    @Override
    public List<Long> listFileIdsByUserIdAndBucket(Long userId, String bucketName) {
        if (userId == null || !StringUtils.hasText(bucketName)) {
            return new ArrayList<>();
        }
        List<Long> ids = ossMapper.listFileIdsByUserIdAndBucket(userId, bucketName.trim());
        return ids == null ? new ArrayList<>() : ids;
    }

    @Override
    public List<String> listFileUrlsByUserIdAndBucket(Long userId, String bucketName) {
        if (userId == null || !StringUtils.hasText(bucketName)) {
            return new ArrayList<>();
        }
        List<OssEntity> files = ossMapper.queryByUserIdAndBucketName(userId, bucketName.trim());
        if (CollectionUtils.isEmpty(files)) {
            return new ArrayList<>();
        }
        List<String> urls = new ArrayList<>(files.size());
        for (OssEntity file : files) {
            if (file == null) {
                urls.add(null);
                continue;
            }
            urls.add(getSafeUrl(file.getBucketName(), file.getObjectName()));
        }
        return urls;
    }

    @Override
    public List<OssBucketFileItemBo> listFileItemsByUserIdAndBucket(Long userId, String bucketName) {
        if (userId == null || !StringUtils.hasText(bucketName)) {
            return new ArrayList<>();
        }
        List<OssEntity> files = ossMapper.queryByUserIdAndBucketName(userId, bucketName.trim());
        if (CollectionUtils.isEmpty(files)) {
            return new ArrayList<>();
        }
        List<OssBucketFileItemBo> out = new ArrayList<>(files.size());
        for (OssEntity file : files) {
            if (file == null || file.getId() == null) {
                continue;
            }
            OssBucketFileItemBo row = new OssBucketFileItemBo();
            row.setFileId(file.getId());
            row.setOriginFileName(file.getOriginFileName());
            row.setUrl(getSafeUrl(file.getBucketName(), file.getObjectName()));
            out.add(row);
        }
        return out;
    }

    @Override
    public boolean deleteFileByFileId(Long fileId) {
        if (fileId == null) {
            return false;
        }
        OssEntity ossEntity = ossMapper.getById(fileId);
        if (ossEntity == null) {
            return false;
        }
        try {
            minioUtils.removeFile(ossEntity.getBucketName(), ossEntity.getObjectName());
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

    private void ensureUploadSuccess(ObjectWriteResponse response, String objectName) {
        if (response == null || !StringUtils.hasText(response.etag())) {
            throw new IllegalStateException("minio upload response invalid, objectName=" + objectName);
        }
    }
}
