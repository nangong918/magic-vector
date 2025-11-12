package com.openapi.component.manager.realTimeChat;

import lombok.Getter;
import lombok.Setter;

/**
 * @author 13225
 * @date 2025/11/11 12:28
 */
public class VisionContext {
    @Getter
    @Setter
    // 是否完成视觉处理, 避免视觉模型本身就返回null的问题
    private boolean isVisionFinished = false;
    private String visionResult = "";

    public String getVisionResult(){
        if (isVisionFinished) {
            return visionResult;
        }
        return "";
    }

    public void setVisionResult(String visionResult){
        this.visionResult = visionResult;
        isVisionFinished = true;
    }

    public void reset(){
        isVisionFinished = false;
        visionResult = "";
    }
}
