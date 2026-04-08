package com.openapi.service.model;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author 13225
 * @date 2025/10/29 15:51
 */
public interface VLService {
    @NotNull
    String vlSingleFileBase64(@NotNull String base64Image, @NotNull String userQuestion) throws NoApiKeyException, UploadFileException;

    @NotNull String vlListFileBase64(@NotNull List<String> base64ImageList, @NotNull String userQuestion) throws NoApiKeyException, UploadFileException;

    @NotNull String vlVideoBase64(@NotNull String base64Video, @NotNull String userQuestion) throws NoApiKeyException, UploadFileException;
}
