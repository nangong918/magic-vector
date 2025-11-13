package com.openapi.component.manager.realTimeChat;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/13 13:06
 */
public class VLManager {
    /// 会话信息
    public String userId;
    public String agentId;

    /// 收到的视觉数据
    public String videoBase64;
    public List<String> imagesBase64 = new ArrayList<>();

    public void resetAll(){
        resetVideo();
        resetImages();
    }
    public void resetVideo(){
        videoBase64 = null;
    }
    public void resetImages(){
        imagesBase64.clear();
    }

    /**
     * 是否为空
     * @return  true: 空
     */
    public boolean isEmpty(){
        return videoBase64 == null && imagesBase64.isEmpty();
    }
}
