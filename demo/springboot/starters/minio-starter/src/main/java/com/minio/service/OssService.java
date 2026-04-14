package com.minio.service;

import com.minio.domain.bo.OssBucketFileItemBo;
import com.minio.domain.entity.OssEntity;
import com.minio.domain.bo.BatchUploadResult;
import com.minio.domain.bo.UploadItemResult;
import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * @author 13225
 * @date 2025/4/9 11:58
 */
public interface OssService {

    OssEntity getFileInfoByFileId(Long ossId);

    BatchUploadResult uploadFiles(List<MultipartFile> files, Long userId, String bucketName);

    BatchUploadResult uploadLocalFiles(List<File> files, Long userId, String bucketName);

    List<OssEntity> listUserFiles(Long userId, String sortBy, Integer offset, Integer size);

    /**
     * 通过fileIds获取图片List<Url>
     * @param fileIds           文件idList
     * @return                  List<Url>
     *     支持Long为null返回null的url
     */
    @NonNull
    List<String> getFileUrlsByFileIds(List<Long> fileIds);

    boolean updateFileNameByFileId(Long fileId, String newFileName);

    UploadItemResult updateFileContentByFileId(Long fileId, MultipartFile file);

    int deleteFilesByFileIds(List<Long> fileIds);

    int deleteAllFilesByUserId(Long userId);

    int countFilesByUserId(Long userId);

    List<String> listBucketNamesByUserId(Long userId);

    List<Long> listFileIdsByUserIdAndBucket(Long userId, String bucketName);

    List<String> listFileUrlsByUserIdAndBucket(Long userId, String bucketName);

    List<OssBucketFileItemBo> listFileItemsByUserIdAndBucket(Long userId, String bucketName);

    // 根据fileId删除
    boolean deleteFileByFileId(Long fileId);

}
