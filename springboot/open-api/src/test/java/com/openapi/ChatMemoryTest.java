package com.openapi;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Chat 记忆测试：
 * 待测试内容：
 * 1. ChatMemory的临时上下文记忆
 * 2. Mysql存储久远上下文记忆
 * 3. RAG 向量数据库(Milvus)索引上下问记忆
 */
@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class ChatMemoryTest {

    @Autowired
    public DashScopeChatModel chatModel;

    @Test
    public void helloWorldTests(){
        log.info("chatModel: {}", chatModel);
    }

    @Test
    public void chatModelChatMemoryTest(){
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        String conversationId = "001";

        String systemPrompt = "你是一个聊天助手叫做uimi，回复尽量精简，一两句话内。";


        // 首次交互
        UserMessage userMessage1 = new UserMessage("你好啊，你是谁？ 我叫做czy，记住我的名字");
        chatMemory.add(conversationId, userMessage1);
        Prompt prompt1 = Prompt.builder()
                .messages(chatMemory.get(conversationId))
                .build().augmentSystemMessage(systemPrompt);
        ChatResponse response1 = chatModel.call(prompt1);
        chatMemory.add(conversationId, response1.getResult().getOutput());

        System.out.println("首次交互: " + response1.getResult().getOutput().getText());

        // 第二次交互
        UserMessage userMessage2 = new UserMessage("我叫什么名字？你还记得你叫什么名字吗？");
        chatMemory.add(conversationId, userMessage2);
        var prompt2 = new Prompt(chatMemory.get(conversationId));
        ChatResponse response2 = chatModel.call(prompt2);
        chatMemory.add(conversationId, response2.getResult().getOutput());
        System.out.println("response2 = " + response2.getResult().getOutput().getText());
    }

    @Test
    public void chatClientChatMemoryTest() {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                // 窗口大小设置为10
                .maxMessages(10)
                .build();

        String conversationId = "002";

        String systemPrompt = "你是一个聊天助手叫做uimi，回复尽量精简，一两句话内。";

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem(systemPrompt)
                .build();

        String answer1 = chatClient.prompt()
                .user("你好啊，你是谁？ 我叫做czy，记住我的名字")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        System.out.println("answer1 = " + answer1);

        String answer2 = chatClient.prompt()
                .user("我叫什么名字？你还记得你叫什么名字吗？")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        System.out.println("answer2 = " + answer2);
    }


}
