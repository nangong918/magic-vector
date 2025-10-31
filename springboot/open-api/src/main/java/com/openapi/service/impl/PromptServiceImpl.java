package com.openapi.service.impl;

import com.openapi.service.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/10/25 12:00
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    /**
     * 在ChatClient的defeatSystem()中调用，让AI知道这是设定而不是聊天
     * @param systemPrompt  系统设定
     * @return              提示String
     */
    @NotNull
    @Override
    public String getSystemPrompt(@NotNull String systemPrompt) {
        if (systemPrompt.isEmpty()){
            return "";
        }
        else {
            return """
                    系统设定：
                    """ +
                    "<" +
                    systemPrompt +
                    ">";
        }
    }

    /**
     * 动态设定，在每次聊天的时候传递，让AI知道SystemMessage而不是UserMessage
     * @param userMessage       用户输入
     * @param systemPrompt      系统设定
     * @return                  提示String
     */
    @Nullable
    @Override
    public Prompt getChatPromptWhitSystemPrompt(@NotNull String userMessage, String systemPrompt) {
        if (userMessage.isEmpty()){
            return null;
        }
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(systemPrompt)){
            messages.add(new SystemMessage(systemPrompt));
        }
        messages.add(new UserMessage(userMessage));
        return new Prompt(messages);
    }

}
