package com.openapi.vl;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.openapi.MainApplication;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.service.PromptService;
import com.openapi.service.VisionChatService;
import com.openapi.service.tools.VisionToolService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Qwen VL 视觉理解测试
 * 参考网址：<a href="https://help.aliyun.com/zh/model-studio/vision?spm=a2c4g.11186623.help-menu-2400256.d_0_2_0.24886be9vqRmce&scm=20140722.H_2845871._.OR_help-T_cn~zh-V_1#071b239d9371c">...</a>
 */
@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class VLTests {

    @Autowired
    private ChatConfig chatConfig;

    @Test
    public void readPromptTest(){
        // 读取JSON文件
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/visionPrompt.json")) {
            if (inputStream == null){
                System.err.println("JSON文件不存在");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // 使用Gson解析JSON
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                // 获取systemPrompt字段
                String systemPrompt = jsonObject.get("systemPrompt").getAsString();

                System.out.println("systemPrompt: " + systemPrompt);
                System.out.println("提示词长度: " + systemPrompt.length() + "字");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        String systemPrompt = chatConfig.getVisionPrompt().get("systemPrompt").getAsString();
        System.out.println("systemPrompt: " + systemPrompt);
    }

    @Autowired
    private VisionChatService visionChatService;

    public static String encodeImageToBase64(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    @Test
    public void visionUnderstandTest() throws NoApiKeyException, UploadFileException, IOException {
        String localPath = "C:/Users/13225/Pictures/to.jpg";
        String base64Image = encodeImageToBase64(localPath);

        String result = visionChatService.callWithFileBase64(
                base64Image,
                "你现在看得到我吗？我给你看一下我的to签？你看得见上面的字吗？我不是很认得出来。"
        );

        /*
        result = {
          "描述": "一位身着粉白洛丽塔风格cos服、头戴鹿角与兔耳的少女，手持道具剑，图片上有手写体文字。",
          "visionAgent回复": "我看得见您图片上的字！虽然有些是手写体，但能辨认出‘10w粉啦!!’、‘石治宇’、‘天天开心!!’、‘感谢陪伴’和‘BY:雪梨语’。这些文字像是粉丝互动或致谢的签名（to签），表达庆祝和感恩之情。"
        }
        */
        System.out.println("result = " + result);
    }

    @Test
    public void visionUnderstandTest2() throws NoApiKeyException, UploadFileException, IOException {
        String localPath = "C:/Users/13225/Pictures/message.png";
        String base64Image = encodeImageToBase64(localPath);

        String result = visionChatService.callWithFileBase64(
                base64Image,
                "你看看这个聊天记里，对方是不是很高冷啊，不理我。"
        );

        /*
        result = {
          "描述": "手机聊天界面截图，显示与Chatbot的对话记录，对方回复简短且带符号，如'你好啊'、'在吗'、'1111'等。",
          "visionAgent回复": "从对话内容看，对方回复简洁但未完全拒绝交流，比如有‘在吗’和数字‘1111’，可能只是风格随意或测试中，并非高冷不理人。"
        }
        */
        System.out.println("result = " + result);
    }

    @Autowired
    private VisionToolService visionToolService;

    @Test
    public void visionFunctionCallTest(@Autowired DashScopeChatModel chatModel) {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的智能助手niger")
                .build();

        String sentence = "你看得到我手上拿的是什么吗？";

        Flux<String> responseFlux = chatClient.prompt()
                .user(sentence)
                // 添加工具Function Call; MCP
                .tools(visionToolService)
                .stream()
                .content()
                // 3500ms未响应则判定超时，进行重连尝试
                .timeout(Duration.ofMillis(ModelConstant.LLM_CONNECT_TIMEOUT_MILLIS));

        responseFlux.subscribe(
                System.out::println,
                System.err::println,
                () -> System.out.println("完成")
        );

        try {
            Thread.sleep(5_000);
        } catch (Exception e){
            log.error("e: ", e);
        }
    }

    @Autowired
    private DashScopeChatModel chatModel;

    @Test
    public void chatClientFunctionCallTest() {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的智能助手")
                .build();

        String sentence = "请查询一下aibo多少钱，还有多少库存？";

        String result = chatClient.prompt()
                .user(sentence)
                .tools(visionToolService)
                .call()
                .content();

        log.info("result: {}", result);
    }

    private String getCurrentContextParam(){
        Map<String, String> param = Map.of(
                "userId", "test_user",
                "agentId", "1234567890",
                "messageId", "1097654321"/*,
                "timestamp", currentMessageDateTime.toString(),
                "userQuestion", userQuestion*/
        );
        return JSON.toJSONString(param);
    }

    @Autowired
    private PromptService promptService;

    @Test
    public void functionCallTest2() {
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的智能助理ciallo，可以帮我管理我的机械设备")
                .build();

        String sentence1 = "你可以听到我说话吗？";

//        String result1 = chatClient.prompt()
//                .user(sentence1)
//                .tools(visionToolService)
//                .call()
//                .content();
//
//        /*
//         result1: 当然可以听到，我在这里为你提供帮助。你有什么需要管理的机械设备问题吗？
//         */
//        log.info("result1: {}", result1);

        String sentence2 = "你可以听到我说话吗？";
        String systemPrompt = chatConfig.getTextFunctionCallPrompt(getCurrentContextParam());

        log.info("systemPrompt: {}", systemPrompt);

        Prompt prompt2 = promptService.getChatPromptWhitSystemPrompt(sentence2, systemPrompt);
        assert prompt2 != null;
//        String result2 = chatClient.prompt(prompt2)
//                .tools(visionToolService)
//                .call()
//                .content();
//        /*
//         result2: 我能够接收并理解您的语音输入，但目前无法直接“听到”您说话。如果您有任何问题或需要帮助，请随时告诉我，我会尽力协助您！
//         */
//        log.info("result2: {}", result2);

        /*
         result3: 我可以听到你说的话。有什么我可以帮助你的吗？
         */
//        String result3 = chatClient.prompt()
//                .system(systemPrompt)
//                .user(sentence2)
//                .tools(visionToolService)
//                .call()
//                .content();
//        log.info("result3: {}", result3);

        String sentence4 = "你可以看到我吗？";
        String result4 = chatClient.prompt()
                .system(systemPrompt)
                .user(sentence4)
                .tools(visionToolService)
                .call()
                .content();
        /*
         result4: 我来看看，请稍等。
         */
        log.info("result4: {}", result4);

        Prompt prompt3 = promptService.getChatPromptWhitSystemPrompt(sentence4, systemPrompt);
        assert prompt3 != null;

        ToolCallback[] toolCallbacks = ToolCallbacks.from(visionToolService);
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .build();
        String result5 = chatClient.prompt(prompt3)
                .options(chatOptions)
//                .tools(visionToolService)
                .call()
                .content();
        /*
         result4: 我已经成功获取了当前摄像头的画面。你现在的样子我可以看到啦！有什么我可以帮你的吗？
         */
        log.info("result5: {}", result5);

        try {
            Thread.sleep(5_000);
        } catch (Exception e){
            log.error("e: ", e);
        }
    }

    @Test
    public void visionCallTest(){
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的智能助理ciallo，可以帮我管理我的机械设备")
                .build();

        String sentence4 = "你可以看到我吗？";

        String systemPrompt = "我提供你一些参数，是可以用于Function Call调用的参数。是否调用要根据语境意图判断。如果是视觉任内务就调用[调用请求调用前端摄像头]方法，并且在调用此方法之后你只能回复：“我来看看请稍等”。可以用于FunctionCall参数: <{\"messageId\":\"1097654321\",\"userId\":\"test_user\",\"agentId\":\"1234567890\"}>";

        Prompt prompt3 = promptService.getChatPromptWhitSystemPrompt(sentence4, systemPrompt);
        assert prompt3 != null;

        ToolCallback[] toolCallbacks = ToolCallbacks.from(visionToolService);
        ChatOptions chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(toolCallbacks)
                .build();
        String result5 = chatClient.prompt(prompt3)
                .options(chatOptions)
//                .tools(visionToolService)
                .call()
                .content();
        /*
         result4: 我已经成功获取了当前摄像头的画面。你现在的样子我可以看到啦！有什么我可以帮你的吗？
         */
        log.info("result5: {}", result5);

        try {
            Thread.sleep(5_000);
        } catch (Exception e){
            log.error("e: ", e);
        }
    }

}
