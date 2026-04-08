package com.openapi.domain.dto.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VideoUploadCompleteRequest {
    @NotBlank(message = "uploadId不能为空")
    private String uploadId;
    @NotNull(message = "userId不能为空")
    private Long userId;
}
