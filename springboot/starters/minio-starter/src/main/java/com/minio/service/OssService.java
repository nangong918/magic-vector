package com.minio.service;

import com.minio.domain.Do.OssDo;
import com.minio.domain.dto.BatchUploadResult;
import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * @author 13225
 * @date 2025/4/9 11:58
 */
public interface OssService {

    OssDo getFileInfoByFileId(Long ossId);

    BatchUploadResult uploadFiles(List<MultipartFile> files, Long userId, String bucketName);

    BatchUploadResult uploadLocalFiles(List<File> files, Long userId, String bucketName);

    List<OssDo> listUserFiles(Long userId, String sortBy, Integer offset, Integer size);

    /**
     * 通过fileIds获取图片List<Url>
     * @param fileIds           文件idList
     * @return                  List<Url>
     *     支持Long为null返回null的url
     */
    @NonNull
    List<String> getFileUrlsByFileIds(List<Long> fileIds);

    // 根据fileId删除
    boolean deleteFileByFileId(Long fileId);

}
