package com.data.domain.dto.ws.request;

import com.data.domain.constant.chat.RealtimeSystemRequestEventEnum;

public class UploadPhotoRequest {
    public String event = RealtimeSystemRequestEventEnum.UPLOAD_PHOTO.getCode();
    public String agentId;
    public String userId;
    public String messageId;
    public Boolean isHavePhoto;
    public String photoBase64;
    // 是否是最后一个分片
    public boolean isLastFragment = false;
}
