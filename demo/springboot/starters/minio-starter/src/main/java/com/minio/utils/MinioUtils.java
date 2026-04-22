package com.minio.utils;

import com.minio.config.MinioConfig;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * Minio工具类
 * 工具类方法较多且存在兼容性保留接口，统一压制未使用告警
 */
@SuppressWarnings("unused")
@Slf4j
@Component
public class MinioUtils {
    @Autowired
    private MinioConfig minioConfig;
    @Autowired
    private MinioClient minioClient;

    /**
     * 启动SpringBoot容器的时候初始化Bucket
     * 如果没有Bucket则创建
     *
     * @param bucketName 存储桶名称
     * @throws Exception 创建存储桶失败时抛出
     */
    public void createBucket(String bucketName) throws Exception {
        try {
            if (!bucketExists(bucketName)) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
            }
        } catch (Exception e){
            log.error("创建存储桶失败, bucketName: {}", bucketName, e);
            throw e;
        }

    }

    /**
     * 判断Bucket是否存在
     *
     * @param bucketName 存储桶名称
     * @return 存储桶是否存在
     * @throws Exception 调用 MinIO 接口失败时抛出
     */
    public boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
    }


    /**
     * 获得Bucket的策略
     *
     * @param bucketName 存储桶名称
     * @return 存储桶策略字符串
     * @throws Exception 调用 MinIO 接口失败时抛出
     */
    public String getBucketPolicy(String bucketName) throws Exception {
        return minioClient.getBucketPolicy(
                        GetBucketPolicyArgs
                                .builder()
                                .bucket(bucketName)
                                .build()
                );
    }

    /**
     * 获得所有Bucket列表
     *
     * @return Bucket 列表
     * @throws Exception 调用 MinIO 接口失败时抛出
     */
    public List<Bucket> getAllBuckets() throws Exception {
        return minioClient.listBuckets();
    }


    /**
     * 根据bucketName获取其相关信息
     *
     * @param bucketName 存储桶名称
     * @return 匹配的 Bucket（可能为空）
     * @throws Exception 调用 MinIO 接口失败时抛出
     */
    public Optional<Bucket> getBucket(String bucketName) throws Exception {
        return getAllBuckets().stream().filter(b -> b.name().equals(bucketName)).findFirst();
    }

    /**
     * 根据bucketName删除Bucket，true：删除成功； false：删除失败，文件或已不存在
     *
     * @param bucketName 存储桶名称
     * @throws Exception 删除存储桶失败时抛出
     */
    public void removeBucket(String bucketName) throws Exception {
        minioClient.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
    }

    public void removeBucketAll(String bucketName) throws Exception{
        // 列出存储桶中的所有对象并删除
        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .build());

        for (Result<Item> result : results) {
            Item item = result.get();  // 获取 Item 对象
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(item.objectName())
                    .build());
        }

        // 删除存储桶
        removeBucket(bucketName);
    }


    /**
     * 判断文件是否存在
     *
     * @param bucketName 存储桶
     * @param objectName 文件名
     * @return 文件是否存在
     */
    public boolean isObjectExist(String bucketName, String objectName) {
        boolean exist = true;
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("[Minio工具类]>>>> 判断文件是否存在, 异常：", e);
            exist = false;
        }
        return exist;
    }

    /**
     * 判断文件夹是否存在
     *
     * @param bucketName 存储桶
     * @param objectName 文件夹名称
     * @return 文件夹是否存在
     */
    public boolean isFolderExist(String bucketName, String objectName) {
        boolean exist = false;
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(objectName)
                            .recursive(false)
                            .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (item.isDir() && objectName.equals(item.objectName())) {
                    exist = true;
                }
            }
        } catch (Exception e) {
            log.error("[Minio工具类]>>>> 判断文件夹是否存在，异常：", e);
            exist = false;
        }
        return exist;
    }

    /**
     * 根据文件前置查询文件
     *
     * @param bucketName 存储桶
     * @param prefix     前缀
     * @param recursive  是否使用递归查询
     * @return MinioItem 列表
     * @throws Exception     调用 MinIO 接口失败时抛出
     */
    public List<Item> getAllObjectsByPrefix(String bucketName,
                                            String prefix,
                                            boolean recursive) throws Exception {
        List<Item> list = new ArrayList<>();
        Iterable<Result<Item>> objectsIterator = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(recursive)
                        .build());
        if (objectsIterator != null) {
            for (Result<Item> o : objectsIterator) {
                Item item = o.get();
                list.add(item);
            }
        }
        return list;
    }

    /**
     * 获取文件流
     *
     * @param bucketName 存储桶
     * @param objectName 文件名
     * @return 二进制流
     */
    public InputStream getObject(String bucketName, String objectName) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build());
    }

    /**
     * 断点下载
     *
     * @param bucketName 存储桶
     * @param objectName 文件名称
     * @param offset     起始字节的位置
     * @param length     要读取的长度
     * @return 二进制流
     */
    public InputStream getObject(String bucketName, String objectName, long offset, long length) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .offset(offset)
                        .length(length)
                        .build());
    }

    /**
     * 获取路径下文件列表
     *
     * @param bucketName 存储桶
     * @param prefix     文件名称
     * @param recursive  是否递归查找，false：模拟文件夹结构查找
     * @return 二进制流
     */
    public Iterable<Result<Item>> listObjects(String bucketName, String prefix,
                                              boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .recursive(recursive)
                        .build());
    }

    /**
     * 使用MultipartFile进行文件上传
     *
     * @param bucketName  存储桶
     * @param file        文件名
     * @param objectName  对象名
     * @param contentType 类型
     * @return 文件上传结果
     * @throws Exception 上传失败时抛出
     */
    public ObjectWriteResponse uploadFile(String bucketName, MultipartFile file,
                                          String objectName, String contentType) throws Exception {
        InputStream inputStream = file.getInputStream();
        return minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .contentType(contentType)
                        .stream(inputStream, inputStream.available(), -1)
                        .build());
    }

    /**
     * 上传本地文件
     *
     * @param bucketName 存储桶
     * @param objectName 对象名称
     * @param fileName   本地文件路径
     * @return 文件上传结果
     * @throws Exception 上传失败时抛出
     */
    public ObjectWriteResponse uploadLocalFile(String bucketName, String objectName,
                                          String fileName) throws Exception {
        return minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .filename(fileName)
                        .build());
    }

    /**
     * 通过流上传文件
     *
     * @param bucketName  存储桶
     * @param objectName  文件对象
     * @param inputStream 文件流
     */
    public ObjectWriteResponse uploadFile(String bucketName, String objectName, InputStream inputStream) throws Exception {
        return minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, inputStream.available(), -1)
                        .build());
    }

    /**
     * 创建文件夹或目录
     *
     * @param bucketName 存储桶
     * @param objectName 目录路径
     */
    public ObjectWriteResponse createDir(String bucketName, String objectName) throws Exception {
        return minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build());
    }

    /**
     * 获取文件信息, 如果抛出异常则说明文件不存在
     *
     * @param bucketName 存储桶
     * @param objectName 文件名称
     */
    public String getFileStatusInfo(String bucketName, String objectName) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()).toString();
    }

    /**
     * 拷贝文件
     *
     * @param bucketName    存储桶
     * @param objectName    文件名
     * @param srcBucketName 目标存储桶
     * @param srcObjectName 目标文件名
     */
    public ObjectWriteResponse copyFile(String bucketName, String objectName,
                                        String srcBucketName, String srcObjectName) throws Exception {
        return minioClient.copyObject(
                CopyObjectArgs.builder()
                        .source(CopySource.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build())
                        .bucket(srcBucketName)
                        .object(srcObjectName)
                        .build());
    }

    /**
     * 删除文件
     *
     * @param bucketName 存储桶
     * @param objectName 文件名称
     */
    public void removeFile(String bucketName, String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build());
    }

    /**
     * 批量删除文件
     *
     * @param bucketName 存储桶名称
     * @param keys 需要删除的文件key列表
     * @return 删除失败的文件列表及原因 (成功时返回空map)
     */
    public Map<String, String> removeFiles(String bucketName, List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return new HashMap<>();
        }

        // 1. 首先尝试使用批量删除API提高效率
        try {
            List<DeleteObject> deleteObjects = keys.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());

            Iterable<Result<DeleteError>> results =
                    minioClient.removeObjects(
                            RemoveObjectsArgs.builder()
                                    .bucket(bucketName)
                                    .objects(deleteObjects)
                                    .build());

            // 收集删除失败的文件
            Map<String, String> failedDeletes = new HashMap<>();
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                failedDeletes.put(error.objectName(), error.message());
            }

            return failedDeletes;
        } catch (Exception e) {
            log.warn("批量删除API失败，尝试回退到单文件删除模式", e);
            return null;
        }
    }


    /**
     * 获取文件外链
     *
     * @param bucketName 存储桶
     * @param objectName 文件名
     * @param expires    过期时间 <=7 秒 （外链有效时间（单位：秒））
     * @return 文件外链 URL
     * @throws Exception 生成外链失败时抛出
     */
    public String getPresignedObjectUrl(String bucketName, String objectName, Integer expires) throws Exception {
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .expiry(expires)
                .bucket(bucketName)
                .object(objectName)
                .build();

        return minioClient.getPresignedObjectUrl(args);
    }

    private String getServerIp() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress(); // 返回本机 IP 地址
        } catch (Exception e) {
            log.warn("获取本机IP地址失败", e);
            return null;
        }
    }

    /**
     * 获得文件外链,失效时间默认是7天。
     * <ul>
     *   <li>{@code use-nginx-proxy=true}：host=本机局域网 IP，端口取 {@code nginx-public-port}（80/443 自动省略）</li>
     *   <li>{@code use-gateway-proxy=true} 且未走 nginx：→ {@code http(s)://本机IP:gatewayPort/minio-url/...}</li>
     *   <li>两者均为 true 时按 nginx</li>
     * </ul>
     *
     * @param bucketName 存储桶
     * @param objectName 文件名
     * @return 文件外链 URL
     * @throws Exception 生成外链失败时抛出
     */
    public String getPresignedObjectUrl(String bucketName, String objectName) throws Exception {
        GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .method(Method.GET).build();

        String url = minioClient.getPresignedObjectUrl(args);

        String httpPrefix = "http://";
        String httpsPrefix = "https://";
        String endpoint = minioConfig.getEndpoint();

        if (minioConfig.isUseNginxProxy()) {
            if (endpoint.contains(httpsPrefix)) {
                return url.replace(minioConfig.getEndpoint(), httpsPrefix + minioConfig.minioNginxAgentUrl());
            }
            return url.replace(minioConfig.getEndpoint(), httpPrefix + minioConfig.minioNginxAgentUrl());
        }
        if (minioConfig.isUseGatewayProxy()) {
            if (endpoint.contains(httpsPrefix)) {
                return url.replace(minioConfig.getEndpoint(), httpsPrefix + minioConfig.minioGatewayAgentUrl());
            }
            return url.replace(minioConfig.getEndpoint(), httpPrefix + minioConfig.minioGatewayAgentUrl());
        }
        return url;
    }

    /**
     * 将URLDecoder编码转成UTF8
     *
     * @param str 原始 URL 编码字符串
     * @return UTF-8 解码后的字符串
     */
    public String getUtf8ByURLDecoder(String str) {
        String url = str.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    /**
     * inputStream上传文件
     * @param bucketName        bucket名称
     * @param inputStream       文件流
     * @param objectName        文件名称
     * @param contentType       文件类型
     * @return                  文件上传结果
     * @throws Exception        Minio异常
     */
    public ObjectWriteResponse uploadFile(String bucketName, InputStream inputStream,
                                          String objectName, String contentType) throws Exception {
        return minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .contentType(contentType)
                        .stream(inputStream, inputStream.available(), -1)
                        .build());
    }
}
