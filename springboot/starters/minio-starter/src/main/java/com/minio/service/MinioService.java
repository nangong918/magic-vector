package com.minio.service;



import com.minio.domain.ao.FileAo;
import com.minio.domain.ao.FileOptionResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * @author 13225
 * @date 2025/4/22 15:39
 */
public interface MinioService {

    /**
     * 上传文件List
     * 幂等性在上游做
     * @param files             文件List
     * @param bucketName        bucketName
     * @return                  ErrorFileList
     */
    FileOptionResult uploadFiles(List<MultipartFile> files, String userId, String bucketName);

    FileOptionResult uploadImages(List<MultipartFile> files, String userId, String bucketName);

    // fileName + id 生成 fileStorageName
    String getObjectNameB(String id, String fileName);

    /**
     *  上传文件List
     * @param files          文件List
     * @param bucketName     bucketName
     * @return                ErrorFileList
     */
    FileOptionResult uploadFiles(List<File> files, String bucketName);

    FileOptionResult uploadMultipartFiles(List<MultipartFile> files, String bucketName);

    FileOptionResult uploadLoadFiles(List<FileAo> fileAos, String bucketName);

    void deleteBucketAll(String bucketName) throws Exception;

    FileOptionResult uploadMultipartImageFiles(List<MultipartFile> files, String bucketName);

    List<String> getAllBucketNames() throws Exception;

    List<String> getAllBucketNames(String prefix) throws Exception;
}
