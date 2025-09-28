package com.openapi;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.function.Try;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;


@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class ChatTests {

    @Autowired
    DashScopeEmbeddingModel dashScopeEmbeddingModel;
    @Autowired
    DashScopeChatModel dashScopeChatModel;

    @Test
    public void helloWorldTest(){
        System.out.println("chatTest");
        // 检查两个模型
        log.info("dashScopeEmbeddingModel: {}", dashScopeEmbeddingModel);
        log.info("dashScopeChatModel: {}", dashScopeChatModel);
    }

    @Test
    public void chatTest(){
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
//                .defaultOptions(chatOptions)
                .build();

        String userQuestion = "你好啊，你是谁？";

        String response = chatClient.prompt()
                .user(userQuestion)
//                .options(chatOptions)
                .call()
                .content();

        System.out.println("response = " + response);
    }

    // 流式输出
    @Test
    public void chatStreamTest(){
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .build();

        String userQuestion = "你好啊，你是谁？";

        // 获取流式响应
        Flux<String> responseFlux = chatClient.prompt()
                .user(userQuestion)
                .stream()
                .content();

        responseFlux.subscribe(System.out::println);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
