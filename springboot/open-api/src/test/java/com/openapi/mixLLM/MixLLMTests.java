package com.openapi.mixLLM;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.openapi.MainApplication;
import com.openapi.component.manager.mixLLM.MixLLMManager;
import com.openapi.config.ChatConfig;
import com.openapi.domain.ao.mixLLM.McpSwitch;
import com.openapi.domain.ao.mixLLM.MixLLMAudio;
import com.openapi.domain.ao.mixLLM.MixLLMResult;
import com.openapi.interfaces.mixLLM.LLMCallback;
import com.openapi.interfaces.mixLLM.TTSCallback;
import com.openapi.interfaces.model.StreamCallErrorCallback;
import com.openapi.interfaces.model.LLMStateCallback;
import com.openapi.service.model.LLMServiceService;
import com.openapi.service.model.TTSServiceService;
import com.openapi.service.tools.VisionToolService;
import io.reactivex.disposables.Disposable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.SignalType;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Autowired
    private TTSServiceService ttsServiceService;

    @Test
    public void ttsTest() throws InterruptedException {

        MixLLMManager mixLLMManager = new MixLLMManager();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的可爱智能机器小狗vector")
                .build();

        McpSwitch mcpSwitch = new McpSwitch();
        mcpSwitch.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code;
        mcpSwitch.camera = McpSwitch.McpSwitchMode.CLOSE.code;
        mcpSwitch.motion = McpSwitch.McpSwitchMode.FREELY.code;

        String sentence = "看向我，朝我走两步，告诉我最近你过的开心吗？你会撒娇吗？要乖乖的哦。";

        long start = System.currentTimeMillis();
        String result = llmServiceService.mixLLMCall(
                sentence,
                chatClient,
                contextParam.get("agentId"),
                contextParam.toString(),
                mcpSwitch
        );
        System.out.println("llm cost = " + (System.currentTimeMillis() - start));
        System.out.println("result = " + result);

        mixLLMManager.start(result, ttsServiceService, new TTSCallback() {
            @Override
            public void onSubscribeDisposable(Disposable disposable) {
                // 记录disposable 到 contextManager
                log.info("[MixLLMManager] record disposable");
            }

            @Override
            public void onStart(Subscription subscription) {
                log.info("[MixLLMManager] tts start");
            }

            @Override
            public void onNext(MixLLMAudio mixLLMAudio) {
                // 流式音频输出
                log.info("[MixLLMManager] tts onNext, audioLength: {}, events: {}",
                        mixLLMAudio.base64Audio.length(), mixLLMAudio.eventList);
            }

            @Override
            public void onComplete() {
                // 发送TTS_END
                log.info("[MixLLMManager] tts end, 发送TTS_END");
            }

            @Override
            public void onError(Throwable throwable) {
                // 输出日志 + endConversation
                log.error("[MixLLMManager] tts error", throwable);
            }
        }, new LLMCallback() {
            @Override
            public void handleResult(String result) {
                log.info("[MixLLMManager] llm result: {}", result);
            }

            @Override
            public void handleStreamResult(String fragmentResult, long messageId) {
                log.info("[MixLLMManager] llm stream result: {}, messageId: {}", fragmentResult, messageId);
            }
        });

        Thread.sleep(30_000L);
    }

    @Autowired
    private VisionToolService visionToolService;

    @Test
    public void streamTTSTest() throws InterruptedException {
        MixLLMManager mixLLMManager = new MixLLMManager();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是我的可爱智能机器小狗vector")
                .build();

        McpSwitch mcpSwitch = new McpSwitch();
        mcpSwitch.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code;
        mcpSwitch.camera = McpSwitch.McpSwitchMode.CLOSE.code;
        mcpSwitch.motion = McpSwitch.McpSwitchMode.FREELY.code;

        String sentence = "看向我，朝我走两步，告诉我最近你过的开心吗？你会撒娇吗？要乖乖的哦。";

        AtomicInteger errorTimes = new AtomicInteger(0);
        List<Object> tasks = new LinkedList<>();

        LLMStateCallback llmStateCallback = new LLMStateCallback() {
            @Override
            public void onSubscribe(Subscription subscription) {
                System.out.println("开始");
            }

            @Override
            public void onFinish(SignalType signalType) {
                System.out.println("结束");
            }

            @Override
            public void onNext(String fragment) {
                System.out.println("fragment = " + fragment);
            }

            @Override
            public void haveNoSentence() {
                System.err.println("没有句子");
            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("error = " + throwable);
            }
        };

        StreamCallErrorCallback llmErrorCallback = new StreamCallErrorCallback() {
            @Override
            public int @NonNull [] addCountAndCheckIsOverLimit() {
                int errorTime = errorTimes.incrementAndGet();
                System.out.println("errorTime = " + errorTime);
                int isOverLimit = errorTime > 3 ? 1 : 0;
                System.out.println("isOverLimit = " + isOverLimit);
                return new int[]{isOverLimit, errorTime};
            }

            @Override
            public void addTask(Object task) {
                tasks.add(task);
            }

            @Override
            public void endConversation() {
                System.out.println("结束会话");
            }
        };

        var disposable = llmServiceService.mixLLMStreamCallErrorProxy(
                sentence,
                chatClient,
                contextParam.get("agentId"),
                contextParam.toString(),
                mcpSwitch,
                llmStateCallback,
                llmErrorCallback,
                // tools
                visionToolService
        );
        tasks.add(disposable);

        Thread.sleep(30_000L);
    }
}
