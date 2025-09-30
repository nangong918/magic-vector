package com.minio.service.impl;



import cn.hutool.core.util.IdUtil;
import com.minio.constant.OssException;
import com.minio.constant.ViewContentTypeEnum;
import com.minio.domain.ao.ErrorFile;
import com.minio.domain.ao.FileAo;
import com.minio.domain.ao.FileOptionResult;
import com.minio.domain.ao.SuccessFile;
import com.minio.service.MinioService;
import com.minio.utils.MinioUtils;
import io.minio.ObjectWriteResponse;
import io.minio.messages.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author 13225
 * @date 2025/4/22 15:45
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class MinioServiceImpl implements MinioService {

    private final MinioUtils minioUtils;

    @Override
    public FileOptionResult uploadLoadFiles(List<FileAo> filesAo, String bucketName){
        // 内部包含检查是否已经存在的逻辑
        FileOptionResult fileOptionResult = new FileOptionResult();
        List<SuccessFile> successFiles = new ArrayList<>();
        List<ErrorFile> errorFiles = new ArrayList<>();

        try {
            minioUtils.createBucket(bucketName);
        } catch (Exception e) {
            log.error("创建存储桶失败", e);
            throw new OssException("创建存储桶失败");
        }

        for (FileAo fileAo : filesAo){
            if (fileAo == null || !StringUtils.hasText(fileAo.getFilePath())){
                continue;
            }
            String fileStorageMame = IdUtil.getSnowflakeNextId() + "_" + fileAo.getFileName();
            try {
                ObjectWriteResponse response = minioUtils.uploadLocalFile(
                        bucketName,
                        fileStorageMame,
                        fileAo.getFilePath()
                );
                if (response != null){
                    String fileId = String.valueOf(IdUtil.getSnowflakeNextId());
                    successFiles.add(new SuccessFile(fileAo.getFileName(), fileStorageMame, fileAo.getFileSize(), fileId));
                }
            } catch (Exception e){
                log.error("上传文件失败", e);
                errorFiles.add(new ErrorFile(fileAo.getFileName(), "[上传失败]"));
            }
        }

        // 添加fileId
        fileOptionResult.setErrorFiles(errorFiles);
        fileOptionResult.setSuccessFiles(successFiles);
        return fileOptionResult;
    }

    @Override
    public void deleteBucketAll(String bucketName) throws Exception{
        minioUtils.removeBucketAll(bucketName);
    }

    @Override
    public FileOptionResult uploadFiles(List<MultipartFile> files, String userId, String bucketName) {
        // 内部包含检查是否已经存在的逻辑
        FileOptionResult fileOptionResult = new FileOptionResult();
        List<SuccessFile> successFiles = new ArrayList<>();
        List<ErrorFile> errorFiles = new ArrayList<>();
        try {
            minioUtils.createBucket(bucketName);
        } catch (Exception e) {
            log.error("创建存储桶失败, bucketName: {}", bucketName, e);
            throw new OssException("创建存储桶失败");
        }
        for (MultipartFile file : files) {
            if (file == null){
                errorFiles.add(new ErrorFile("", "[文件不能为空]"));
                continue;
            }
            String fileName = IdUtil.getSnowflakeNextId() + "_" + file.getOriginalFilename();
            if (file.getOriginalFilename() == null) {
                errorFiles.add(new ErrorFile("", "[文件名不能为空]"));
                continue;
            }
            try {
                String fileStorageName = getObjectNameB(userId, fileName);
                ObjectWriteResponse response = minioUtils.uploadFile(bucketName, file, fileStorageName, file.getContentType());
                if (response != null){
                    String fileId = String.valueOf(IdUtil.getSnowflakeNextId());
                    successFiles.add(new SuccessFile(fileName, fileStorageName, file.getSize(), fileId));
                }
            } catch (Exception e) {
                log.error("上传文件失败", e);
                errorFiles.add(new ErrorFile(fileName, "[上传失败]"));
            }
        }
        // 添加fileId

        fileOptionResult.setErrorFiles(errorFiles);
        fileOptionResult.setSuccessFiles(successFiles);
        return fileOptionResult;
    }

    @Override
    public FileOptionResult uploadImages(List<MultipartFile> files, String userId, String bucketName) {
        // 内部包含检查是否已经存在的逻辑
        FileOptionResult fileOptionResult = new FileOptionResult();
        List<SuccessFile> successFiles = new ArrayList<>();
        List<ErrorFile> errorFiles = new ArrayList<>();
        try {
            minioUtils.createBucket(bucketName);
        } catch (Exception e) {
            log.error("创建存储桶失败, bucketName: {}", bucketName, e);
            throw new OssException("创建存储桶失败");
        }
        for (MultipartFile file : files) {
            if (file == null){
                errorFiles.add(new ErrorFile("", "[文件不能为空]"));
                continue;
            }
            String fileName = IdUtil.getSnowflakeNextId() + "_" + file.getOriginalFilename();
            if (file.getOriginalFilename() == null) {
                errorFiles.add(new ErrorFile("", "[文件名不能为空]"));
                continue;
            }
            try {
                String fileStorageName = getObjectNameB(userId, fileName);
                ObjectWriteResponse response = minioUtils.uploadFile(
                        bucketName,
                        file,
                        fileStorageName,
                        ViewContentTypeEnum.getContentType(fileName)
                );
                if (response != null){
                    String fileId = String.valueOf(IdUtil.getSnowflakeNextId());
                    successFiles.add(new SuccessFile(fileName, fileStorageName, file.getSize(), fileId));
                }
            } catch (Exception e) {
                log.error("上传文件失败", e);
                errorFiles.add(new ErrorFile(fileName, "[上传失败]"));
            }
        }
        // 添加fileId

        fileOptionResult.setErrorFiles(errorFiles);
        fileOptionResult.setSuccessFiles(successFiles);
        return fileOptionResult;
    }

    @Override
    public String getObjectNameB(String id, String fileName) {
        // id + fileName + 时间戳
        // 保留 fileName 的前 15 个字符
        String shortFileName = fileName.length() > 15 ? fileName.substring(0, 15) : fileName;
        // 获取当前时间戳
        long timestamp = System.currentTimeMillis();
        // 构建待编码字符串
        String input = id + "_" + shortFileName + "_" + timestamp;
        // 使用 Base64 编码
        return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public FileOptionResult uploadFiles(List<File> files, String bucketName) {
        FileOptionResult fileOptionResult = new FileOptionResult();
        List<SuccessFile> successFiles = new ArrayList<>();
        List<ErrorFile> errorFiles = new ArrayList<>();
        if (CollectionUtils.isEmpty(files)){
            return fileOptionResult;
        }
        try {
            minioUtils.createBucket(bucketName);
        } catch (Exception e) {
            log.error("创建存储桶失败", e);
            throw new OssException("创建存储桶失败");
        }
        for (File file : files){
            if (file == null){
                errorFiles.add(new ErrorFile("", "[文件不能为空]"));
                continue;
            }
            String fileName = file.getName();
            if (!StringUtils.hasText(fileName)){
                errorFiles.add(new ErrorFile("", "[文件名不能为空]"));
            }
            try (InputStream inputStream = Files.newInputStream(file.toPath())){
                // 处理 InputStream，例如读取数据
                int data = inputStream.read();
                while (data != -1) {
                    data = inputStream.read();
                }
                fileName = IdUtil.getSnowflakeNextId() + "_" + fileName;
                ObjectWriteResponse response = minioUtils.uploadFile(
                        bucketName,
                        fileName,
                        inputStream
                );
                String fileStorageName = getObjectNameB("-1L", fileName);
                if (response != null){
                    String fileId = String.valueOf(IdUtil.getSnowflakeNextId());
                    successFiles.add(new SuccessFile(fileName, fileStorageName, file.length(), fileId));
                }
            } catch (IOException e){
                log.error("上传文件失败, file转为inputStream失败", e);
                errorFiles.add(new ErrorFile(fileName, "[上传失败]"));
                continue;
            } catch (Exception e) {
                log.error("上传文件失败，minIO存储失败", e);
                errorFiles.add(new ErrorFile(fileName, "[上传失败]"));
                continue;
            }
        }
        fileOptionResult.setErrorFiles(errorFiles);
        fileOptionResult.setSuccessFiles(successFiles);
        return fileOptionResult;
    }

    @Override
    public FileOptionResult uploadMultipartFiles(List<MultipartFile> files, String bucketName) {
        FileOptionResult fileOptionResult = new FileOptionResult();
        List<SuccessFile> successFiles = new ArrayList<>();
        List<ErrorFile> errorFiles = new ArrayList<>();
        if (CollectionUtils.isEmpty(files)){
            return fileOptionResult;
        }
        try {
            minioUtils.createBucket(bucketName);
        } catch (Exception e) {
            log.error("创建存储桶失败", e);
            throw new OssException("创建存储桶失败");
        }

        for (MultipartFile file : files){
            if (file == null){
                errorFiles.add(new ErrorFile("", "[文件不能为空]"));
                continue;
            }
            String fileName = IdUtil.getSnowflakeNextId() + "_" + file.getName();
            log.info("上传文件fileName：{}", fileName);
            try {
                String fileStorageName = getObjectNameB("-1L", fileName);
                ObjectWriteResponse response = minioUtils.uploadFile(bucketName, file, fileStorageName, file.getContentType());
                log.info("上传文件fileStorageName：{}", fileStorageName);
                if (response != null){
                    String fileId = String.valueOf(IdUtil.getSnowflakeNextId());
                    successFiles.add(new SuccessFile(fileName, fileStorageName, file.getSize(), fileId));
                }
            } catch (Exception e) {
                log.error("上传文件失败, file转为inputStream失败", e);
                errorFiles.add(new ErrorFile(fileName, "[上传失败]"));
                continue;
            }
        }
        fileOptionResult.setErrorFiles(errorFiles);
        fileOptionResult.setSuccessFiles(successFiles);
        return fileOptionResult;
    }

    @Override
    public FileOptionResult uploadMultipartImageFiles(List<MultipartFile> files, String bucketName) {
        FileOptionResult fileOptionResult = new FileOptionResult();
        List<SuccessFile> successFiles = new ArrayList<>();
        List<ErrorFile> errorFiles = new ArrayList<>();
        if (CollectionUtils.isEmpty(files)){
            return fileOptionResult;
        }
        try {
            minioUtils.createBucket(bucketName);
        } catch (Exception e) {
            log.error("创建存储桶失败", e);
            throw new OssException("创建存储桶失败");
        }

        for (MultipartFile file : files){
            if (file == null){
                errorFiles.add(new ErrorFile("", "[文件不能为空]"));
                continue;
            }
            String fileName = IdUtil.getSnowflakeNextId() + "_" + file.getOriginalFilename();
            log.info("上传文件fileName：{}", fileName);
            try {
                String fileStorageName = getObjectNameB("-1L", fileName);
                ObjectWriteResponse response = minioUtils.uploadFile(
                        bucketName, file,
                        fileStorageName,
                        ViewContentTypeEnum.getContentType(fileName)
                );
                log.info("上传文件fileStorageName：{}", fileStorageName);
                if (response != null){
                    String fileId = String.valueOf(IdUtil.getSnowflakeNextId());
                    successFiles.add(new SuccessFile(fileName, fileStorageName, file.getSize(), fileId));
                }
            } catch (Exception e) {
                log.error("上传文件失败, file转为inputStream失败", e);
                errorFiles.add(new ErrorFile(fileName, "[上传失败]"));
                continue;
            }
        }
        fileOptionResult.setErrorFiles(errorFiles);
        fileOptionResult.setSuccessFiles(successFiles);
        return fileOptionResult;
    }

    @Override
    public List<String> getAllBucketNames() throws Exception {
        List<Bucket> buckets = minioUtils.getAllBuckets();
        List<String> bucketNames = new ArrayList<>();
        for (Bucket bucket : buckets) {
            bucketNames.add(bucket.name());
        }
        return bucketNames;
    }

    @Override
    public List<String> getAllBucketNames(String prefix) throws Exception {
        List<Bucket> buckets = minioUtils.getAllBuckets();
        List<String> bucketNames = new ArrayList<>();
        for (Bucket bucket : buckets) {
            if (bucket.name().startsWith(prefix)) {
                bucketNames.add(bucket.name());
            }
        }
        return bucketNames;
    }
}
