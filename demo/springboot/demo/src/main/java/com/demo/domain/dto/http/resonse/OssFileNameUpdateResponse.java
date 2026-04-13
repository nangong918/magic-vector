package com.demo.domain.dto.http.resonse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class OssFileNameUpdateResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fileId;
    private String newFileName;
    private Boolean updated;
    private String message;
}
