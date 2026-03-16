package com.openapi.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VideoUploadChunkRequest {
    @NotBlank(message = "uploadId不能为空")
    private String uploadId;
    @NotNull(message = "userId不能为空")
    private Long userId;
    @NotNull(message = "chunkIndex不能为空")
    @Min(value = 0, message = "chunkIndex必须>=0")
    private Integer chunkIndex;
    @NotNull(message = "offset不能为空")
    @Min(value = 0, message = "offset必须>=0")
    private Long offset;
}
