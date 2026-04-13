package com.demo.domain.dto.http.resonse;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class OssFileContentUpdateResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long fileId;
    private String originFileName;
    private String url;
    private Boolean updated;
    private String message;
}
