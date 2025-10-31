package com.openapi.domain.dto.ws.request;

import com.openapi.domain.constant.realtime.RealtimeSystemResponseEventEnum;
import lombok.Data;

@Data
public class UploadPhotoRequest {
    public String event = RealtimeSystemResponseEventEnum.UPLOAD_PHOTO.getCode();
    public String agentId;
    public String userId;
    public String messageId;
    public Boolean isHavePhoto;
    public String photoBase64;
}
