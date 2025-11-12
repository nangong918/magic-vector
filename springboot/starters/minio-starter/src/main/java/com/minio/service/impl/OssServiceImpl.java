package com.minio.service.impl;



import com.minio.constant.OssException;
import com.minio.domain.Do.OssDo;
import com.minio.domain.ao.FileIsExistAo;
import com.minio.domain.ao.FileIsExistResult;
import com.minio.domain.ao.FileNameAo;
import com.minio.domain.ao.FileOptionResult;
import com.minio.domain.ao.SuccessFile;
import com.minio.mapper.OssMapper;
import com.minio.service.MinioService;
import com.minio.service.OssService;
import com.minio.utils.MinioUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/4/9 9:50
 * mysql存储文件信息。作为文件索引以及文件幂等性。
 * 先上传oss再上传mysql，这样的保证文件和数据一致性。
 * 删除也是一样。
 * oss和mysql禁止使用事务。因为速度差距太大，oss上传过程中会对mysql全程上锁
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OssServiceImpl implements OssService {

    private final OssMapper ossMapper;
    private final MinioUtils minioUtils;
    private final MinioService minioService;

    @Override
    public OssDo getFileInfoByFileId(String fileId) {
        return ossMapper.getById(fileId);
    }

    @Override
    public OssDo getFileInfoByBucketNameAndFileName(String bucketName, String fileName) {
        return ossMapper.getByObjectNameAndBucketName(fileName, bucketName);
    }


    @Override
    public List<FileIsExistResult> checkFilesExistForResult(List<FileIsExistAo> fileIsExistAos){
        if (CollectionUtils.isEmpty(fileIsExistAos)){
            return new ArrayList<>();
        }
        List<FileIsExistResult> result = new ArrayList<>(fileIsExistAos.size());
        for (FileIsExistAo fileIsExistAo : fileIsExistAos){
            FileIsExistResult fileIsExistResult = new FileIsExistResult();
            OssDo ossDo = ossMapper.getByObjectNameAndBucketName(
                    fileIsExistAo.getObjectName(),
                    fileIsExistAo.getBucketName()
            );
            boolean isExist = ossDo != null && ossDo.getId() != null;
            fileIsExistResult.setIsExist(isExist);
            if (isExist){
                fileIsExistResult.setFileId(ossDo.getId());
            }
            result.add(fileIsExistResult);
        }
        return result;
    }

    @Override
    public FileOptionResult uploadFiles(List<MultipartFile> files, String userId, String bucketName) {
        if (CollectionUtils.isEmpty(files)){
            return new FileOptionResult();
        }
        FileOptionResult result = minioService.uploadFiles(files, userId, bucketName);
        // 成功的存储到数据库
        uploadFilesRecord(result.getSuccessFiles(), userId, bucketName);
//        // 失败的加入到list
//        result.getErrorFiles().addAll(errorFileList);
        return result;
    }


    @Override
    public void uploadFilesRecord(List<SuccessFile> files, String userId, String bucketName) {
        for (SuccessFile successFile : files){
            // oss
            OssDo ossFileDo = new OssDo();
            ossFileDo.setObjectName(successFile.getObjectName());
            ossFileDo.setBucketName(bucketName);
            // 已经设置了id
            if (successFile.getFileId() != null){
                // 检查id是否已经上传，避免重复上传
                OssDo checkExistFileDo = ossMapper.getById(successFile.getFileId());
                if (checkExistFileDo != null && checkExistFileDo.getId() != null){
                    log.info("文件已经存在，id为：{}", checkExistFileDo.getId());
                    continue;
                }
                ossFileDo.setId(successFile.getFileId());
                // 插入
                Long fileId = ossMapper.insert(ossFileDo);
                log.info("文件插入成功，fileId:{}, successFile.id:{}", fileId, successFile.getFileId());
            }
            else {
                // 插入
                ossMapper.insert(ossFileDo);
                // 设置fileId
                successFile.setFileId(ossFileDo.getId());
            }
        }
    }

    // Todo 改为先redis缓存，redis缓存未命中走minIO
    @NonNull
    @Override
    public List<String> getFileUrlsByFileIds(List<String> fileIds) {
        List<String> fileUrls = new LinkedList<>();
        for (String fileId : fileIds){
            if (fileId == null){
                fileUrls.add(null);
                continue;
            }
            OssDo ossFileDo = ossMapper.getById(fileId);
            addUrlToList(fileUrls, ossFileDo);
        }
        return fileUrls;
    }

    private void addUrlToList(List<String> fileUrls, OssDo ossFileDo){
        if (ossFileDo != null){
            String bucketName = ossFileDo.getBucketName();
            String objectName = ossFileDo.getObjectName();
            log.info("bucketName:{}, fileId: {}, objectName:{}",
                    bucketName,
                    ossFileDo.getId(),
                    objectName
            );
            String url = getFileUrl(bucketName, objectName);
            fileUrls.add(url);
        }
        else {
            // 数据长度对齐
            fileUrls.add(null);
        }
    }

    private String getFileUrl(String bucketName, String objectName){
        try{
            if (!StringUtils.hasText(bucketName)){
                throw new OssException("存储桶不存在");
            }
            if (!StringUtils.hasText(objectName)){
                throw new OssException("文件不存在");
            }
            return minioUtils.getPresignedObjectUrl(bucketName, objectName);
        } catch (Exception e){
            log.warn("获取文件地址失败", e);
            return "";
        }
    }


    @Override
    public boolean deleteFileByFileId(String fileId) {
        OssDo ossFileDo = ossMapper.getById(fileId);
        if (ossFileDo != null){
            try{
                // oss删除
                minioUtils.removeFile(ossFileDo.getBucketName(), ossFileDo.getObjectName());
                // mysql删除
                ossMapper.delete(fileId);
                return true;
            } catch (Exception e){
                log.warn("删除文件失败", e);
                throw new OssException("删除文件失败");
            }
        }
        return false;
    }



    public static void main(String[] args) {
        String fileName = "test.txt";
        Long userId = 123L;
        // userId + fileName + 时间戳
        // 获取当前时间戳
        long timestamp = System.currentTimeMillis();
        // 构建待编码字符串
        String input = userId + "_" + fileName + "_" + timestamp;
        // 使用 Base64 编码
        String fileStorageName = Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
        System.out.println("fileStorageName = " + fileStorageName);
        try{
            byte[] decodedBytes = Base64.getDecoder().decode(fileStorageName);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = decoded.split("_");
            if (parts.length == 3) {
                FileNameAo fileNameAo = new FileNameAo();
                fileNameAo.setUserId(Long.parseLong(parts[0]));
                fileNameAo.setFileName(parts[1]);
                fileNameAo.setTimestamp(Long.parseLong(parts[2]));
                System.out.println("fileNameAo.toJsonString() = " + fileNameAo);
            } else {
                throw new OssException("文件名格式错误");
            }
        } catch (Exception e){
            throw new OssException("文件名解码失败");
        }
    }
}
