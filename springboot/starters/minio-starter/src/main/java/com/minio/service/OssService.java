package com.minio.service;



import com.minio.domain.Do.OssDo;
import com.minio.domain.ao.FileIsExistAo;
import com.minio.domain.ao.FileIsExistResult;
import com.minio.domain.ao.FileOptionResult;
import com.minio.domain.ao.SuccessFile;
import lombok.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author 13225
 * @date 2025/4/9 11:58
 */
public interface OssService {

    // id -> ossDo
    OssDo getFileInfoByFileId(String ossId);

    /**
     * userId + bucketName + fileName -> OssDo
     * @param bucketName        bucketName
     * @param objectName        objectName
     * @return                  OssFile
     */
    OssDo getFileInfoByBucketNameAndFileName(String bucketName, String objectName);


    List<FileIsExistResult> checkFilesExistForResult(List<FileIsExistAo> fileIsExistAos);

    /**
     * 上传文件List
     * @param files             文件List
     * @param userId            用户id
     * @param bucketName        bucketName
     * @return                  ErrorFileList
     */
    FileOptionResult uploadFiles(List<MultipartFile> files, String userId, String bucketName);

    /**
     * 成功的存储到数据库
     * 方法存储成功之后会对将files的id赋值
     * 如果不想被自动赋值请先提前赋值，内部会检查文件id是否为null
     * @param files             文件List
     * @param userId            用户id
     * @param bucketName        bucketName
     */
    void uploadFilesRecord(List<SuccessFile> files, String userId, String bucketName);

    /**
     * 通过fileIds获取图片List<Url>
     * @param fileIds           文件idList
     * @return                  List<Url>
     *     支持Long为null返回null的url
     */
    @NonNull
    List<String> getFileUrlsByFileIds(List<String> fileIds);


    // 根据fileId删除
    boolean deleteFileByFileId(String fileId);

}
