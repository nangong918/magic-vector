package com.openapi.vl;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.openapi.MainApplication;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.service.VisionChatService;
import com.openapi.service.VisionToolService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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

    @Test
    public void chatClientFunctionCallTest(@Autowired DashScopeChatModel chatModel) {
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

}
