package com.openapi.component.manager.mixLLM;

import com.alibaba.fastjson.JSON;
import com.openapi.component.manager.realTimeChat.RealtimeChatContextManager;
import com.openapi.domain.ao.mixLLM.MixLLMAudio;
import com.openapi.domain.ao.mixLLM.MixLLMResult;
import com.openapi.domain.constant.realtime.RealtimeResponseDataTypeEnum;
import com.openapi.domain.dto.ws.response.RealtimeChatTextResponse;
import com.openapi.interfaces.mixLLM.LLMCallback;
import com.openapi.interfaces.mixLLM.TTSCallback;
import com.openapi.connect.websocket.manager.PersistentConnectMessageManager;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Subscription;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author 13225
 * @date 2025/11/10 18:05
 * LLM结果管理者
 */
@Slf4j
public class MixLLMManager extends AbstractMixLLMManager{

    // 缓存结果(不能放在抽象类，不能多个对象用一个单例)
    protected final Queue<MixLLMResult> mixLLMResults = new ConcurrentLinkedQueue<>();
    protected final StringBuilder sentenceBuilder = new StringBuilder();

    @Override
    protected Queue<MixLLMResult> getMixLLMResultsQueue() {
        return mixLLMResults;
    }

    @Override
    protected StringBuilder getSentenceBuilder() {
        return sentenceBuilder;
    }

    @Override
    public void reset() {
        mixLLMResults.clear();
        sentenceBuilder.setLength(0);
    }

    public static TTSCallback getDefaultTTSCallback(
            RealtimeChatContextManager chatContextManager,
            PersistentConnectMessageManager webSocketMessageManager){
        return new TTSCallback() {
            @Override
            public void onSubscribeDisposable(Disposable disposable) {
                chatContextManager.addChatTask(disposable);
            }

            @Override
            public void onStart(Subscription subscription) {
                // tts Start
                log.info("[TTSCallback] tts start");
                // 发送开始标识
                Map<String, String> responseMap = new HashMap<>();
                responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.START_TTS.getType());
                responseMap.put(RealtimeResponseDataTypeEnum.DATA, RealtimeResponseDataTypeEnum.START_TTS.getType());
                String startResponse = JSON.toJSONString(responseMap);
                webSocketMessageManager.submitMessage(
                        chatContextManager.agentId,
                        startResponse
                );
            }

            @Override
            public void onNext(MixLLMAudio mixLLMAudio) {
                // 发送到设备端
                // 音频数据
                Optional.ofNullable(mixLLMAudio)
                        .map(it -> it.base64Audio)
                        .ifPresent(
                                audioBase64Data -> {
                                    Map<String, String> responseMap = new HashMap<>();
                                    responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.AUDIO_CHUNK.getType());
                                    responseMap.put(RealtimeResponseDataTypeEnum.DATA, audioBase64Data);
                                    String response = JSON.toJSONString(responseMap);
                                    webSocketMessageManager.submitMessage(
                                            chatContextManager.agentId,
                                            response
                                    );
                                    log.info("[TTSCallback] 发送TTS数据，tts data length: {}", audioBase64Data.length());
                                }
                        );

                // 事件数据
                Optional.ofNullable(mixLLMAudio)
                        .map(it -> it.eventList)
                        .filter(list -> !list.isEmpty())
                        .ifPresent(
                                eventList -> {
                                    String eventListJSONStr = JSON.toJSONString(eventList);
                                    Map<String, String> responseMap = new HashMap<>();
                                    responseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.EVENT_LIST.getType());
                                    responseMap.put(RealtimeResponseDataTypeEnum.DATA, eventListJSONStr);
                                    String response = JSON.toJSONString(responseMap);
                                    webSocketMessageManager.submitMessage(
                                            chatContextManager.agentId,
                                            response
                                    );
                                    log.info("[TTSCallback] 发送EventList事件数据，eventList: {}", eventListJSONStr);
                                }
                        );
            }

            @Override
            public void onComplete() {
                // tts end
                log.info("[TTSCallback] tts end");
                chatContextManager.endConversation();
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("[TTSCallback] tts error", throwable);
                chatContextManager.endConversation();
            }
        };
    }

    public static LLMCallback getDefaultLLMCallback(
            @NotNull RealtimeChatContextManager chatContextManager,
            @NotNull PersistentConnectMessageManager webSocketMessageManager){
        return new LLMCallback() {
            @Override
            public void handleResult(String result) {
                // 消息发送给设备端
                RealtimeChatTextResponse agentFragmentResponse = chatContextManager.getCurrentFragmentAgentResponse(result);

                // 发送消息给Client
                String agentFragmentResponseJson = JSON.toJSONString(agentFragmentResponse);
                Map<String, String> fragmentResponseMap = new HashMap<>();
                fragmentResponseMap.put(RealtimeResponseDataTypeEnum.TYPE, RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE.getType());
                fragmentResponseMap.put(RealtimeResponseDataTypeEnum.DATA, agentFragmentResponseJson);
                String response = JSON.toJSONString(fragmentResponseMap);
                webSocketMessageManager.submitMessage(
                        chatContextManager.agentId,
                        response
                );

                log.info("[LLMCallback] LLM-Tools结果：{}", result);
            }
        };
    }
}
