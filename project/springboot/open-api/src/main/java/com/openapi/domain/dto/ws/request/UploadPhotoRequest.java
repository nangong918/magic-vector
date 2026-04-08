package com.openapi.domain.dto.ws.request;

import com.openapi.domain.constant.realtime.RealtimeSystemRequestEventEnum;
import lombok.Data;

@Data
public class UploadPhotoRequest {
    public String event = RealtimeSystemRequestEventEnum.UPLOAD_PHOTO.getCode();
    public String agentId;
    public String userId;
    public String messageId;
    public Boolean isHavePhoto;
    public Integer currentPhotoIndex;
    // ws无法一次性接收全部的数据，所以进行分片。
    public String photoBase64;
    // 是否是最后一个分片
    public boolean isLastFragment = false;
}
