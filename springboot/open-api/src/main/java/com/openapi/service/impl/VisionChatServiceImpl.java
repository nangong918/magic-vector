package com.openapi.service.impl;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.google.gson.JsonElement;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.service.VisionChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

/**
 * @author 13225
 * @date 2025/10/29 15:51
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionChatServiceImpl implements VisionChatService {

    private final MultiModalConversation multiModalConversation;
    private final ChatConfig chatConfig;

    @Override
    public String callWithFileBase64(@NotNull String base64Image, @NotNull String userQuestion) throws NoApiKeyException, UploadFileException {
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        new HashMap<>() {{
                            put("image", "data:image/png;base64," + base64Image);
                        }},
                        new HashMap<>() {{
                            put("text", userQuestion);
                        }}
                )).build();

        String systemPrompt = Optional.ofNullable(chatConfig.getVisionPrompt())
                .map(it -> it.get("systemPrompt"))
                .map(JsonElement::getAsString)
                .orElse("");
        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(
                        Collections.singletonMap("text", systemPrompt)))
                .build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(chatConfig.getApiKey())
                .model(ModelConstant.Vision_Model)
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();

        MultiModalConversationResult result = multiModalConversation.call(param);
        return result.getOutput().getChoices()
                .getFirst()
                .getMessage().getContent()
                .getFirst()
                .get("text").toString();
    }

}
