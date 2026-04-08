package com.openapi.component.manager.realTimeChat;

import com.openapi.domain.dto.ws.response.RealtimeChatTextResponse;
import lombok.NonNull;

/**
 * @author 13225
 * @date 2025/11/6 15:49
 * 设计模式：桥接模式：拆分为多个接口
 * 获取试试聊天的响应
 */
public interface IRealTimeChatResponseManager {
    /**
     * 到目前为止的Agent响应
     * @return  text响应
     */
    @NonNull
    RealtimeChatTextResponse getUpToNowAgentResponse();

    /**
     * 到目当前的Agent片段响应
     * @param fragmentText   片段文本
     * @param messageId       消息id
     * @return  text响应
     */
    @NonNull
    RealtimeChatTextResponse getCurrentFragmentAgentResponse(
            @NonNull String fragmentText,
            long messageId
    );

    /**
     * 获取用户语音识别结果响应
     * @param sstResult     用户语音识别结果
     * @return              text响应
     */
    @NonNull
    RealtimeChatTextResponse getUserSTTResultResponse(@NonNull String sstResult);

    /**
     * 获取用户文本结果响应
     * @param userChatText      用户文本结果
     * @return                  text响应
     */
    @NonNull
    RealtimeChatTextResponse getUserTextResponse(@NonNull String userChatText);
}
