package com.openapi.service.model;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/10/29 15:51
 */
public interface VLService {
    @NotNull
    String callWithFileBase64(@NotNull String base64Image, @NotNull String userQuestion) throws NoApiKeyException, UploadFileException;
}
