package com.openapi.mixLLM;

import com.openapi.MainApplication;
import com.openapi.config.ChatConfig;
import com.openapi.domain.ao.realtimeChat.McpSwitch;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * @author 13225
 * @date 2025/11/10 16:55
 */


@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class MixLLMTests {

    @Autowired
    private ChatConfig config;

    @Test
    public void promptTest() {
        Map<String, String> contextParam = Map.of("agentId", "111", "userId", "222", "messageId", "333");
        String systemPrompt = config.getMixLLMSystemPrompt(contextParam.toString(), new McpSwitch());
        log.info("systemPrompt: {}", systemPrompt);
    }



}
