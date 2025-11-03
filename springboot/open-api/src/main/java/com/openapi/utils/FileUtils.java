package com.openapi.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

/**
 * @author 13225
 * @date 2025/11/3 11:22
 */
public class FileUtils {

    /**
     * 将MultipartFile图片转为Base64字符串
     * @param img 上传的图片文件
     * @return Base64编码字符串（带格式前缀，如"data:image/png;base64,"）
     * @throws Exception 处理过程中的异常（如文件读取失败）
     */
    @NotNull
    public static String multipartFileToBase64(MultipartFile img) throws Exception {
        if (img == null || img.isEmpty()) {
            throw new IllegalArgumentException("图片文件不能为空");
        }

        // 1. 获取文件字节数组
        byte[] fileBytes = img.getBytes();

        // 2. 编码为Base64字符串（不含前缀）
        String base64Encoded = Base64.getEncoder().encodeToString(fileBytes);

        // 3. 获取文件ContentType（如image/png、image/jpeg），用于拼接前缀
        String contentType = img.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("文件不是有效的图片类型");
        }

        // 4. 拼接完整的Base64格式（带前缀，方便前端直接使用）
        return "data:" + contentType + ";base64," + base64Encoded;
    }

}
