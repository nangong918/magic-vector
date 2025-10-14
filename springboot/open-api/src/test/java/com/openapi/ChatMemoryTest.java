package com.openapi;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
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

}
