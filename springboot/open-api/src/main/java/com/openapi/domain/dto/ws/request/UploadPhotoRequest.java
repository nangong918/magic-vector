package com.openapi.domain.dto.ws.request;

import lombok.Data;

@Data
public class UploadPhotoRequest {
    public String agentId;
    public String userId;
    public String messageId;
    public Boolean isHavePhoto;
    public String photoBase64;
}
