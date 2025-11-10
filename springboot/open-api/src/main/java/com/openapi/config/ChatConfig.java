package com.openapi.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.openapi.domain.ao.realtimeChat.McpSwitch;
import com.openapi.domain.ao.realtimeChat.MixLLMResult;
import com.openapi.domain.constant.tools.AICallEnum;
import com.openapi.domain.constant.tools.EmojiEvent;
import com.openapi.domain.constant.tools.MoodEvent;
import com.openapi.domain.constant.tools.MotionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Configuration
public class ChatConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    @Bean
    public ChatClient getChatClient(@Autowired DashScopeChatModel dashScopeChatModel) {
        return ChatClient.builder(dashScopeChatModel)
//                .defaultOptions(chatOptions)
                .build();
    }

    @Bean
    public Recognition getRecognition() {
        return new Recognition();
    }

    // 此方法是TTS和VL模型的公用模型：多模态模型
    @Bean
    public MultiModalConversation getMultiModalConversation() {
        return new MultiModalConversation();
    }

    public static final String SYSTEM_PROMPT_KEY = "systemPrompt";

    @Bean("visionPrompt")
    public com.google.gson.JsonObject getVisionPrompt() {
        // 读取JSON文件
        String jsonFilePath = "/ai/visionPrompt.json";
        try (InputStream inputStream = getClass().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null){
                log.warn("JSON文件不存在: {}", jsonFilePath);
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // 使用Gson解析JSON
                Gson gson = new Gson();

                return gson.fromJson(reader, com.google.gson.JsonObject.class);
            }
        } catch (Exception e) {
            log.error("JSON文件解析错误", e);
        }
        return null;
    }

    public String getVisionLLMPrompt(String visionResult) {
        // 读取JSON文件
        String jsonFilePath = "/ai/visionLLMPrompt.json";
        try (InputStream inputStream = getClass().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null){
                log.warn("JSON文件不存在: {}", jsonFilePath);
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // 使用Gson解析JSON
                Gson gson = new Gson();
                String systemPrompt = Optional.ofNullable(gson.fromJson(reader, com.google.gson.JsonObject.class))
                        .map(it -> it.get(SYSTEM_PROMPT_KEY))
                        .map(JsonElement::getAsString)
                        .orElse("");
                if (systemPrompt.isEmpty()) {
                    log.warn("visionLLMPrompt.json文件内容为空");
                    return systemPrompt + "visionAgent识别结果: <前端获取照片失败，无识别结果>";
                }
                else if (!StringUtils.hasText(visionResult)) {
                    log.warn("visionResult为空");
                    return systemPrompt + "visionAgent识别结果: <前端获取照片失败，无识别结果>";
                } else {
                    return systemPrompt + "visionAgent识别结果: <" + visionResult + ">";
                }
            }
        } catch (Exception e) {
            log.error("JSON文件解析错误", e);
        }
        return null;
    }

    public String getTextFunctionCallPrompt(String param) {
        // 读取JSON文件
        String jsonFilePath = "/ai/textPrompt.json";
        try (InputStream inputStream = getClass().getResourceAsStream(jsonFilePath)) {
            if (inputStream == null){
                log.warn("JSON文件不存在: {}", jsonFilePath);
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // 使用Gson解析JSON
                Gson gson = new Gson();
                String systemPrompt = Optional.ofNullable(gson.fromJson(reader, com.google.gson.JsonObject.class))
                        .map(it -> it.get(SYSTEM_PROMPT_KEY))
                        .map(JsonElement::getAsString)
                        .orElse("");

                if (systemPrompt.isEmpty()) {
                    return systemPrompt + "可以用于FunctionCall参数: <" + param + ">";
                }
                else if (!StringUtils.hasText(param)) {
                    log.warn("param为空");
                }
                return systemPrompt + "可以用于FunctionCall参数: <" + param + ">";
            } catch (Exception e) {
                log.error("JSON文件解析错误", e);
            }
        } catch (Exception e){
            log.error("JSON文件解析错误", e);
        }
        return null;
    }

    public String getMixLLMSystemPrompt(String contextParam){
        String jsonFilePath = "/ai/mixLLMPrompt.txt";
        try (InputStream inputStream = ChatConfig.class.getResourceAsStream(jsonFilePath)) {
            if (inputStream == null){
                System.err.println("文件不存在");
                return null;
            }
            // 读取text -> string
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // 参数替换
            PromptTemplate promptTemplateObj = PromptTemplate.builder()
                    .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                    .template(text)
                    .build();
            Map<String, Object> params = new HashMap<>();

            StringBuilder invokeConfigSb = new StringBuilder();

            McpSwitch mcpSwitch = new McpSwitch();
            mcpSwitch.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code;
            mcpSwitch.camera = McpSwitch.McpSwitchMode.COMMANDS.code;
            mcpSwitch.motion = McpSwitch.McpSwitchMode.FREELY.code;

            McpSwitch.McpSwitchMode cameraSwitchMode = McpSwitch.McpSwitchMode.getByCode(mcpSwitch.camera);
            McpSwitch.McpSwitchMode motionSwitchMode = McpSwitch.McpSwitchMode.getByCode(mcpSwitch.motion);
            McpSwitch.McpSwitchMode emojiAndMoodSwitchMode = McpSwitch.McpSwitchMode.getByCode(mcpSwitch.emojiAndMood);

            if (!McpSwitch.McpSwitchMode.CLOSE.equals(cameraSwitchMode)){
                invokeConfigSb.append("\n相机选项包括：").append("照片拍摄。")
//                        .append(AICallEnum.getAIDocs(VisionEvent.class))
                ;
            }
            if (!McpSwitch.McpSwitchMode.CLOSE.equals(motionSwitchMode)){
                invokeConfigSb.append("\n运动选项包括：")
                        .append(AICallEnum.getAIDocs(MotionEvent.class))
                ;
            }
            if (!McpSwitch.McpSwitchMode.CLOSE.equals(emojiAndMoodSwitchMode)){
                invokeConfigSb.append("\n表情选项包括：")
                        .append(AICallEnum.getAIDocs(EmojiEvent.class))
                        .append("心情选项包括：")
                        .append(AICallEnum.getAIDocs(MoodEvent.class))
                ;
            }

            String invokeConfig = invokeConfigSb.toString();
            params.put("json_format", MixLLMResult.getInvocationRules());
            params.put("context_param", contextParam);
            params.put("invoke_setting", mcpSwitch.getAICallInstructions());
            params.put("invoke_config", invokeConfig.isEmpty() ? "无" : invokeConfig);

            return promptTemplateObj.render(params);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        return null;
    }

    // 文件读取测试
    public static void main(String[] args) {
        ChatConfig chatConfig = new ChatConfig();
        String mixLLMSystemPrompt = chatConfig.getMixLLMSystemPrompt(
                """
                        userId: 123,
                        agentId: 456
                        """
        );
        System.out.println(mixLLMSystemPrompt);
    }
}
