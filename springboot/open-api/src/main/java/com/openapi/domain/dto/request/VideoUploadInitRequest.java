package com.openapi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VideoUploadInitRequest {
    @NotBlank(message = "userId不能为空")
    private String userId;
    @NotBlank(message = "fileName不能为空")
    private String fileName;
    @NotNull(message = "fileSize不能为空")
    private Long fileSize;
}
