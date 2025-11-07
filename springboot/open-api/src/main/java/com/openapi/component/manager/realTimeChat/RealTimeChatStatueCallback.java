package com.openapi.component.manager.realTimeChat;

import com.openapi.domain.constant.realtime.RealTimeChatStatue;

/**
 * @author 13225
 * @date 2025/11/7 15:46
 */
public interface RealTimeChatStatueCallback {
    void onStatueChange(RealTimeChatStatue realTimeChatStatue);
}
