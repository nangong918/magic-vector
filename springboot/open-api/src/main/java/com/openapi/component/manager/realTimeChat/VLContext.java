package com.openapi.component.manager.realTimeChat;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/13 13:06
 */
@Slf4j
public class VLContext {
    /// 会话信息
    public String userId;
    public String agentId;

    /// 收到的视觉数据
    @Getter
    private String videoBase64;
    @Getter
    private final List<String> imagesBase64 = new ArrayList<>();

    public void setVideoBase64(String videoBase64){
        if (videoBase64 != null && !videoBase64.isEmpty()){
            this.videoBase64 = videoBase64;
        }
        else {
            log.warn("[VLContext::setVideoBase64] 视频数据为空");
        }
    }

    /// 状态 + 结果
    private boolean isVisionFinished = false;
    private String visionResult = "";

    public void setVisionResult(String visionResult){
        this.visionResult = visionResult;
        isVisionFinished = true;
        resetVideo();
        resetImages();
    }
    public String getVisionResult(){
        if (isVisionFinished) {
            return visionResult;
        }
        return "";
    }

    public void resetAll(){
        resetVideo();
        resetImages();
        isVisionFinished = false;
        visionResult = "";
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

    public boolean isHasVideo(){
        return videoBase64 != null && !videoBase64.isEmpty();
    }
}
