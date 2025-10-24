package com.openapi;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import com.openapi.component.manager.OptimizedSentenceDetector;
import com.openapi.config.AgentConfig;
import com.openapi.converter.ChatMessageConverter;
import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.error.AgentExceptions;
import com.openapi.domain.dto.request.ChatRequest;
import com.openapi.domain.exception.AppException;
import com.openapi.service.AgentService;
import com.openapi.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


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

        String systemPrompt = "你只能输出自然语言，不要输出表情等特殊符号" + OptimizedSentenceDetector.END_PUNCTUATION;
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

    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private ChatMessageConverter chatMessageConverter;
    @Autowired
    private AgentConfig agentConfig;
    @Autowired
    private AgentService agentService;

    @Test
    public void chatRealtimeContextTest(){
        final String agentId = "1979114877567455232";
        AgentAo agentAo = agentService.getAgentById(agentId);
        if (agentAo == null || agentAo.getAgentId() == null){
            throw new AppException(AgentExceptions.AGENT_NOT_EXIST);
        }

        // 设定
        String description = Optional.ofNullable(agentAo.getAgentVo())
                .map(agentVo -> agentVo.description)
                .orElseGet(() -> {
                    log.warn("Agent 没有设定，使用默认设定");
                    return ModelConstant.SYSTEM_PROMPT;
                });

        ChatMemory chatMemory = agentConfig.chatMemory();

        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem(description)
                .build();

        // 预先加载10条历史聊天记录
        List<ChatMessageDo> chatMessageDos = chatMessageService.getLast10Messages(agentId);
        // 将历史消息添加到ChatMemory中
        if (!chatMessageDos.isEmpty()) {

            // 按时间正序排列，确保对话顺序正确 （前端展示是最新的放在第0个，而此处是最新的放在最后一个添加，所以需要重排序）
            List<ChatMessageDo> sortedMessages = chatMessageDos.stream()
                    .sorted(Comparator.comparing(ChatMessageDo::getChatTime))
                    .toList();

            List<Message> historyMessages = chatMessageConverter.chatMessageDoListToMessageList(sortedMessages);
            for (Message message : historyMessages) {
                chatMemory.add(agentId, message);
            }
        }

        String userQuestion = "你好啊";

        Flux<String> responseFlux = chatClient.prompt()
                .user(userQuestion)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, agentId))
                .stream()
                .content();

        StringBuffer textBuffer = new StringBuffer();
        AtomicInteger fragmentCount = new AtomicInteger(0);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());


        // 订阅流式响应并处理
        responseFlux.subscribe(

                // 处理每个流片段
                fragment -> {
                    // 将新片段添加到缓冲区
                    textBuffer.append(fragment);
                    log.info("[缓冲区累计]: {} 字符", textBuffer.length());

                    // 尝试从缓冲区提取完整句子并输出
                    String completeSentence;
                    while ((completeSentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer)) != null) {
                        /// tts
                        if (StringUtils.hasText(completeSentence)){
                            log.info("[TTS] 完整句子: {}", completeSentence);
                        }
                    }

                    // 显示当前缓冲区剩余内容
                    if (!textBuffer.isEmpty()) {
                        log.info("[缓冲区剩余]: {}", textBuffer);
                    }

                    // 更新最后活跃时间
                    startTime.set(System.currentTimeMillis());
                },

                // 处理错误
                error -> {
                    long totalTime = System.currentTimeMillis() - startTime.get();
                    log.error("\n[LLM 错误] 总耗时: {}ms, 片段总数: {}", totalTime, fragmentCount.get(), error);
                },

                // 处理完成
                () -> {
                    long totalTime = System.currentTimeMillis() - startTime.get();
                    log.info("\n[LLM 结束] 总耗时: {}ms, 片段总数: {}, 总字符数: {}",
                            totalTime, fragmentCount.get(), textBuffer.length());

                    // 处理缓冲区中可能剩余的不完整内容
                    if (!textBuffer.isEmpty()) {
                        log.info("[最终剩余未完成内容]: {}", textBuffer);
                    }

                    log.info("[LLM 流式响应完全结束]");
                }
        );


        // 休眠 20秒
        try {
            Thread.sleep(20_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 超时http connect reset 问题
    @Test
    public void chatConnectResetTest(){
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .build();

        String userQuestion = "你好啊，我叫czy，你是谁？";
        log.info("开始第1次调用");
        Flux<String> responseFlux1 = chatClient.prompt()
                .user(userQuestion)
                .stream()
                .content();

        responseFlux1.subscribe(
                fragment -> {
                    log.info("[LLM1 响应]: {}", fragment);
                },
                error -> {
                    log.error("[LLM1 错误]", error);
                },
                () -> {
                    log.info("[LLM1 流式响应完全结束]");
                }
        );

        long sleepTime = 1_000 * 60 * 5;
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        log.info("开始第2次调用");
        String userQuestion2 = "你还记得我叫什么名字吗？";
        Flux<String> responseFlux2 = chatClient.prompt()
                .user(userQuestion2)
                .stream()
                .content();

        responseFlux2.subscribe(
                fragment -> {
                    log.info("[LLM2 响应]: {}", fragment);
                },
                error -> {
                    log.error("[LLM2 错误]", error);
                },
                () -> {
                    log.info("[LLM2 流式响应完全结束]");
                }
        );

        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String userQuestion3 = "你叫什么名字啊？";
        log.info("开始第3次调用");
        Flux<String> responseFlux3 = chatClient.prompt()
                .user(userQuestion3)
                .stream()
                .content();

        responseFlux3.subscribe(
                fragment -> {
                    log.info("[LLM3 响应]: {}", fragment);
                },
                error -> {
                    log.error("[LLM3 错误]", error);
                },
                () -> {
                    log.info("[LLM3 流式响应完全结束]");
                }
        );

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        /*
2025-10-24T10:42:15.908+08:00  INFO 26940 --- [open-api] [           main] com.openapi.ChatTests                    : 开始第1次调用
2025-10-24T10:42:16.970+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 你好
2025-10-24T10:42:16.972+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 呀
2025-10-24T10:42:16.974+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: ，
2025-10-24T10:42:16.975+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: czy
2025-10-24T10:42:16.975+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: ！👋 我
2025-10-24T10:42:16.976+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 是通义千
2025-10-24T10:42:17.009+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 问（Qwen
2025-10-24T10:42:17.011+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: ），是阿里巴巴集团
2025-10-24T10:42:17.055+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 旗下的通义实验室
2025-10-24T10:42:17.099+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 自主研发的超大规模
2025-10-24T10:42:17.151+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 语言模型。你可以
2025-10-24T10:42:17.160+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 叫我Qwen，
2025-10-24T10:42:17.189+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 或者直接叫我小
2025-10-24T10:42:17.291+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 通也行～
2025-10-24T10:42:17.476+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 很高兴认识你！
2025-10-24T10:42:17.516+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: ✨

我特别
2025-10-24T10:42:17.572+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 喜欢和大家聊天
2025-10-24T10:42:17.582+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 、学习新知识
2025-10-24T10:42:17.623+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: ，还能帮你写
2025-10-24T10:42:17.678+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 故事、写公
2025-10-24T10:42:17.691+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 文、写邮件
2025-10-24T10:42:17.722+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 、写剧本，
2025-10-24T10:42:17.773+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 甚至编程、做
2025-10-24T10:42:17.783+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 数学题都行
2025-10-24T10:42:17.903+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 哦！有什么我可以
2025-10-24T10:42:17.919+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 帮你的吗？
2025-10-24T10:42:17.920+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 响应]: 😊
2025-10-24T10:42:17.943+08:00  INFO 26940 --- [open-api] [oundedElastic-1] com.openapi.ChatTests                    : [LLM1 流式响应完全结束]
2025-10-24T10:47:16.319+08:00  INFO 26940 --- [open-api] [           main] com.openapi.ChatTests                    : 开始第2次调用
2025-10-24T10:47:17.323+08:00  INFO 26940 --- [open-api] [           main] com.openapi.ChatTests                    : 开始第3次调用
2025-10-24T10:47:17.623+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: 我
2025-10-24T10:47:17.640+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: 叫
2025-10-24T10:47:17.644+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: 通
2025-10-24T10:47:17.653+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: 义
2025-10-24T10:47:17.681+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: 千问，英文
2025-10-24T10:47:17.752+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: 名叫Qwen。
2025-10-24T10:47:17.773+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: 你可以叫我Qwen
2025-10-24T10:47:17.823+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: 。很高兴认识你
2025-10-24T10:47:17.838+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 响应]: ！😊
2025-10-24T10:47:17.865+08:00  INFO 26940 --- [open-api] [oundedElastic-3] com.openapi.ChatTests                    : [LLM3 流式响应完全结束]
         */

        // 上述可见第二次因为Connect Reset丢失了，然后第三次正常，所以如果Connect Reset之后需要立刻重试2~3次
    }
}
