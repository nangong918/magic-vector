package com.data.domain.dto.ws.request;

import com.data.domain.constant.chat.RealtimeSystemResponseEventEnum;

public class UploadPhotoRequest {
    public String event = RealtimeSystemResponseEventEnum.UPLOAD_PHOTO.getCode();
    public String agentId;
    public String userId;
    public String messageId;
    public Boolean isHavePhoto;
    public String photoBase64;
}
