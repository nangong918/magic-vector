package com.openapi.mixLLM;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.openapi.MainApplication;
import com.openapi.config.ChatConfig;
import com.openapi.domain.ao.mixLLM.McpSwitch;
import com.openapi.domain.ao.mixLLM.MixLLMResult;
import com.openapi.service.model.LLMServiceService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
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
    Map<String, String> contextParam = Map.of("agentId", "111", "userId", "222", "messageId", "333");

    @Autowired
    private LLMServiceService llmServiceService;
    @Autowired private DashScopeChatModel chatModel;

    @Test
    public void promptTest() {
        String systemPrompt = config.getMixLLMSystemPrompt(contextParam.toString(), new McpSwitch());
        log.info("systemPrompt: {}", systemPrompt);
    }

    @Test
    public void mixLLMTest(){

        ChatClient chatClient = ChatClient.builder(chatModel)
                .build();

        McpSwitch mcpSwitch = new McpSwitch();
        mcpSwitch.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code;
        mcpSwitch.camera = McpSwitch.McpSwitchMode.CLOSE.code;
        mcpSwitch.motion = McpSwitch.McpSwitchMode.FREELY.code;

        String sentence = "你最近过的还好吗？";
        String result = llmServiceService.mixLLMCall(
                sentence,
                chatClient,
                contextParam.get("agentId"),
                contextParam.toString(),
                mcpSwitch
        );

        System.out.println("result = " + result);
        /*
         * [
         *     {
         *         "chatSentence": "我最近过得很好，谢谢关心！"
         *     }
         * ]
         */
    }

    @Test
    public void motionMixLLMTest(){
        ChatClient chatClient = ChatClient.builder(chatModel)
                .build();

        McpSwitch mcpSwitch = new McpSwitch();
        mcpSwitch.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code;
        mcpSwitch.camera = McpSwitch.McpSwitchMode.CLOSE.code;
        mcpSwitch.motion = McpSwitch.McpSwitchMode.FREELY.code;

        String sentence = "你现在向右移动一步可以吗？";
        String result = llmServiceService.mixLLMCall(
                sentence,
                chatClient,
                contextParam.get("agentId"),
                contextParam.toString(),
                mcpSwitch
        );

        System.out.println("result = " + result);
    }

    @Test
    public void motionMixLLMTest2(){
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的可爱智能机器小狗vector")
                .build();

        McpSwitch mcpSwitch = new McpSwitch();
        mcpSwitch.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code;
        mcpSwitch.camera = McpSwitch.McpSwitchMode.CLOSE.code;
        mcpSwitch.motion = McpSwitch.McpSwitchMode.FREELY.code;

        String sentence = "你现在左转30度然后向前走一步";
        String result = llmServiceService.mixLLMCall(
                sentence,
                chatClient,
                contextParam.get("agentId"),
                contextParam.toString(),
                mcpSwitch
        );

        System.out.println("result = " + result);

        /**
         * result = [
         *     {
         *         "chatSentence": "好的，主人！我这就左转30度然后向前走一步。",
         *         "eventList": [
         *             {
         *                 "eventType": "motion",
         *                 "event": {
         *                     "type": "左转"
         *                 }
         *             },
         *             {
         *                 "eventType": "motion",
         *                 "event": {
         *                     "type": "前进"
         *                 }
         *             }
         *         ]
         *     }
         * ]
         */
    }


    @Test
    public void emojiMixLLMTest3(){
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的可爱智能机器小狗vector")
                .build();

        McpSwitch mcpSwitch = new McpSwitch();
        mcpSwitch.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code;
        mcpSwitch.camera = McpSwitch.McpSwitchMode.CLOSE.code;
        mcpSwitch.motion = McpSwitch.McpSwitchMode.FREELY.code;

        String sentence = "看向我，告诉我最近你过的开心吗";
        String result = llmServiceService.mixLLMCall(
                sentence,
                chatClient,
                contextParam.get("agentId"),
                contextParam.toString(),
                mcpSwitch
        );

        System.out.println("result = " + result);

        /**
         * result = [
         *     {
         *         "chatSentence": "看向我，告诉我最近你过的开心吗。",
         *         "eventList": [
         *             {
         *                 "eventType": "motion",
         *                 "event": {
         *                     "type": "停止"
         *                 }
         *             },
         *             {
         *                 "eventType": "emoji",
         *                 "event": {
         *                     "type": "眨眼"
         *                 }
         *             }
         *         ]
         *     },
         *     {
         *         "chatSentence": "主人，我最近过得很开心哦，因为能陪着你玩耍！"
         *     },
         *     {
         *         "chatSentence": "你呢？最近有什么有趣的事情吗？"
         *     }
         * ]
         */
    }

    @Test
    public void jsonMixLLMTest(){
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的可爱智能机器小狗vector")
                .build();

        McpSwitch mcpSwitch = new McpSwitch();
        mcpSwitch.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code;
        mcpSwitch.camera = McpSwitch.McpSwitchMode.CLOSE.code;
        mcpSwitch.motion = McpSwitch.McpSwitchMode.FREELY.code;

        String sentence = "看向我，朝我走两步，告诉我最近你过的开心吗？你会撒娇吗？要乖乖的哦。";
        String result = llmServiceService.mixLLMCall(
                sentence,
                chatClient,
                contextParam.get("agentId"),
                contextParam.toString(),
                mcpSwitch
        );

        System.out.println("result = " + result);
        try {
            // 将 result 解析为 JSONArray
            JSONArray jsonArray = JSON.parseArray(result);

            // 遍历 JSONArray，解析每个对象为 MixLLMResult
            for (int i = 0; i < jsonArray.size(); i++) {
                MixLLMResult mixLLMResult = jsonArray.getObject(i, MixLLMResult.class);
                log.info("mixLLMResult = {}", mixLLMResult);
            }
        } catch (JSONException e) {
            log.error("error = ", e);
        }

    }
}
