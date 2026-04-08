package com.openapi.component.manager.realTimeChat;

import com.openapi.domain.constant.realtime.RealTimeChatState;

/**
 * @author 13225
 * @date 2025/11/7 15:46
 */
public interface RealTimeChatStateCallback {
    void onStateChange(RealTimeChatState realTimeChatState);
}
