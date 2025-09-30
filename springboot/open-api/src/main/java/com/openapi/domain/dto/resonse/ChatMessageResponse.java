package com.openapi.domain.dto.resonse;

import com.openapi.domain.Do.ChatMessageDo;
import lombok.Data;

import java.util.List;

/**
 * @author 13225
 * @date 2025/9/30 16:23
 */
@Data
public class ChatMessageResponse {
    private List<ChatMessageDo> chatMessages;
}
