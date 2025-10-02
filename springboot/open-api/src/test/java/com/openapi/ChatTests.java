package com.openapi;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.openapi.component.manager.OptimizedSentenceDetector;
import com.openapi.domain.dto.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
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
                .uri("/test/stream")
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


    // 分句测试
    @Test
    public void chatStreamSentenceTest() {
        String text = "你好，欢迎使用HanLP汉语处理包！";
        List<Term> termList = HanLP.segment(text);
        System.out.println(termList);
    }

    /**
     * 句末标点集合（可根据需求扩展，如添加省略号“……”）
     */
    private static final String END_PUNCTUATION = "。？！.!?";


    /**
     * 检测并提取缓冲区中的第一个完整句子
     * @param textBuffer 流式文本缓冲区
     * @return 第一个完整句子，如果没有则返回null
     */
    public String detectAndExtractFirstSentence(StringBuffer textBuffer) {
        // 基础校验
        if (textBuffer == null || textBuffer.isEmpty()) {
            return null;
        }

        String currentText = textBuffer.toString();
        List<Term> termList = HanLP.segment(currentText);

        // 遍历分词结果，寻找第一个完整句子的结束位置
        int sentenceEndIndex = findFirstSentenceEndIndex(termList);

        if (sentenceEndIndex > 0) {
            // 提取完整句子
            String completeSentence = currentText.substring(0, sentenceEndIndex);
            // 从缓冲区移除已提取的句子
            textBuffer.delete(0, sentenceEndIndex);
            return completeSentence;
        }

        return null;
    }

    /**
     * 寻找第一个完整句子的结束索引
     */
    private int findFirstSentenceEndIndex(List<Term> termList) {
        if (termList.size() < 2) {
            return -1; // 至少需要两个词才能构成一个句子（内容+标点）
        }

        int currentPosition = 0;

        // 遍历所有词，找到第一个符合条件的句末标点
        for (int i = 0; i < termList.size(); i++) {
            Term currentTerm = termList.get(i);
            currentPosition += currentTerm.word.length();

            // 检查当前词是否为句末标点
            if (Nature.w.equals(currentTerm.nature) &&
                    END_PUNCTUATION.contains(currentTerm.word)) {

                // 确保标点前有有效内容
                if (i > 0) {
                    Term previousTerm = termList.get(i - 1);
                    if (!previousTerm.word.trim().isEmpty()) {
                        // 找到第一个完整句子的结束位置
                        return currentPosition;
                    }
                }
            }
        }

        return -1; // 未找到完整句子
    }

    // ------------------- 你的测试方法扩展 -------------------
    @Test
    public void chatStreamSentenceTest2() {
        // 模拟流式场景：分3次拼接文本片段
        StringBuffer textBuffer = new StringBuffer();

        // 片段1：“你好，”（逗号不是句末标点，不构成完整句子）
        textBuffer.append("你好，");
        String sentence1 = detectAndExtractFirstSentence(textBuffer);
        System.out.println("片段1处理结果：" + (sentence1 == null ? "无完整句子" : "完整句子：" + sentence1));
        System.out.println("处理后缓冲区：" + textBuffer); // 输出：你好，

        // 片段2：“欢迎使用HanLP汉语处理包！”（感叹号是句末标点，构成完整句子）
        textBuffer.append("欢迎使用HanLP汉语处理包！");
        String sentence2 = detectAndExtractFirstSentence(textBuffer);
        System.out.println("片段2处理结果：" + (sentence2 == null ? "无完整句子" : "完整句子：" + sentence2)); // 输出：完整句子：你好，欢迎使用HanLP汉语处理包！
        System.out.println("处理后缓冲区：" + textBuffer); // 输出：（空）

        // 片段3：“这是新的句子。测试结束”（前半部分是完整句子，后半部分不是）
        textBuffer.append("这是新的句子。测试结束");
        String sentence3 = detectAndExtractFirstSentence(textBuffer);
        System.out.println("片段3处理结果：" + (sentence3 == null ? "无完整句子" : "完整句子：" + sentence3)); // 输出：完整句子：这是新的句子。
        System.out.println("处理后缓冲区：" + textBuffer); // 输出：测试结束
    }


    @Autowired
    OptimizedSentenceDetector optimizedSentenceDetector;

    // 流式句子输出
    @Test
    public void chatStreamSeqTest(){
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .build();

        String systemPrompt = "你只能输出自然语言，不要输出表情等特殊符号，在输出完一句话之后需加上如下标点符号之一：" + OptimizedSentenceDetector.END_PUNCTUATION;
        String userQuestion = "你好啊，你是谁？";

        // 获取流式响应
        Flux<String> responseFlux = chatClient.prompt(systemPrompt)
                .user(userQuestion)
                .stream()
                .content();

        StringBuffer textBuffer = new StringBuffer();

        // 订阅流式响应并处理
        responseFlux.subscribe(
                // 处理每个流片段
                fragment -> {
                    // 将新片段添加到缓冲区
                    textBuffer.append(fragment);
                    System.out.println("\n[接收到片段]: " + fragment);

                    // 尝试从缓冲区提取完整句子并输出
                    String completeSentence;
                    while ((completeSentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer)) != null) {
                        System.out.println("\n[提取到完整句子]: " + completeSentence);
                        // 在这里可以调用TTS服务生成音频
                        // generateAudio(completeSentence);
                    }

                    // 显示当前缓冲区剩余内容
                    if (!textBuffer.isEmpty()) {
                        System.out.println("[缓冲区剩余]: " + textBuffer);
                    }
                },
                // 处理错误
                error -> System.err.println("流式处理错误: " + error.getMessage()),
                // 处理完成
                () -> {
                    System.out.println("\n[流式响应结束]");
                    // 处理缓冲区中可能剩余的不完整内容
                    if (!textBuffer.isEmpty()) {
                        System.out.println("[最终剩余未完成内容]: " + textBuffer);
                    }
                }
        );

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
