package com.openapi;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.openapi.domain.dto.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


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

    @Autowired
    private WebClient.Builder webClientBuilder;

    /**
     * 测试流式请求 (测试通过)
     * @throws InterruptedException 测试超时
     * === 开始接收流数据 ===
     * 收到数据: 你好
     * 收到数据: 呀！🌟
     * 收到数据: 我是
     * 收到数据: Q
     * 收到数据: wen，也就是通义千
     * 收到数据: 问，是阿里
     * 收到数据: 云研发的超大规模语言
     * 收到数据: 模型。你可以叫我小
     * 收到数据: 千或者Qwen都可以
     * 收到数据: 哦！我特别
     * 收到数据: 喜欢和人类朋友
     * 收到数据: 聊天，不仅能一起
     * 收到数据: 探讨问题，还能帮你
     * 收到数据: 写故事、写公
     * 收到数据: 文、写邮件，
     * 收到数据: 甚至写剧本呢
     * 收到数据: ！虽然我可能
     * 收到数据: 不是最完美的，但我会
     * 收到数据: 认真倾听你的每一个
     * 收到数据: 问题，尽我所能提供
     * 收到数据: 帮助。就像现在，
     * 收到数据: 很高兴能在这里遇见
     * 收到数据: 你！有什么我可以
     * 收到数据: 帮到你的吗？
     * 收到数据: 😊
     * === 流数据接收完成 ===
     * === 测试结束 ===
     */
    @Test
    public void chatStreamRequestTest() throws InterruptedException {
        WebClient webClient = webClientBuilder.baseUrl("http://localhost:48888").build();
        ChatRequest request = new ChatRequest();
        request.setQuestion("你好啊，你是谁？");

        CountDownLatch latch = new CountDownLatch(1);

        webClient.post()
                .uri("/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnSubscribe(subscription -> {
                    System.out.println("=== 开始接收流数据 ===");
                })
                .doOnNext(data -> {
                    System.out.println("收到数据: " + data);
                    // 这里可以添加数据验证逻辑
                })
                .doOnComplete(() -> {
                    System.out.println("=== 流数据接收完成 ===");
                    latch.countDown();
                })
                .doOnError(error -> {
                    System.err.println("发生错误: " + error.getMessage());
                    latch.countDown();
                })
                .subscribe();

        // 等待一段时间让流处理完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("测试超时");
        }

        System.out.println("=== 测试结束 ===");
    }

}
