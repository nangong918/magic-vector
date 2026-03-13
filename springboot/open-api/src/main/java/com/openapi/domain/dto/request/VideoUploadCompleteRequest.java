package com.openapi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VideoUploadCompleteRequest {
    @NotBlank(message = "uploadId不能为空")
    private String uploadId;
    @NotBlank(message = "userId不能为空")
    private String userId;
}
