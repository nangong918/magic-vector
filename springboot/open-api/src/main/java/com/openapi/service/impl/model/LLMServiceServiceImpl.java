package com.openapi.service.impl.model;

import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.domain.constant.error.AgentExceptions;
import com.openapi.domain.exception.AppException;
import com.openapi.interfaces.model.LLMStateCallback;
import com.openapi.service.PromptService;
import com.openapi.service.model.LLMServiceService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * @author 13225
 * @date 2025/11/8 14:21
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMServiceServiceImpl implements LLMServiceService {

    private final ChatConfig chatConfig;
    private final PromptService promptService;

    /**
     * LLM 流式调用
     * @param sentence                  输入
     * @param chatClient                chatClient
     * @param agentId                   聊天id
     * @param currentContextParam       上下文参数
     * @param callback                  LLM回调
     * @param functionCallTools         工具s
     * @return                          管理任务的Disposable
     */
    @Override
    public reactor.core.Disposable LLMStreamCall(
            @NonNull String sentence,
            @NonNull ChatClient chatClient,
            @NonNull String agentId,
            @Nullable String currentContextParam,
            @NonNull LLMStateCallback callback,
            @NonNull Object... functionCallTools) {
        if (sentence.isEmpty()){
            callback.haveNoSentence();
            return null;
        }

        if (currentContextParam == null){
            currentContextParam = "";
        }

        String systemPrompt = chatConfig.getTextFunctionCallPrompt(currentContextParam);
        Prompt prompt = promptService.getChatPromptWhitSystemPrompt(
                sentence,
                systemPrompt
        );

        if (prompt == null){
            log.error("[LLM 提示词] 获取失败");
            throw new AppException(AgentExceptions.CHAT_CAN_NOT_BE_NULL);
        }

        Flux<String> responseFlux;

        if (functionCallTools.length > 0){
            responseFlux = chatClient.prompt(prompt)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, agentId))
                    // 添加工具Function Call; MCP
                    .tools(functionCallTools)
                    .stream()
                    .content()
                    // 3500ms未响应则判定超时，进行重连尝试
                    .timeout(Duration.ofMillis(ModelConstant.LLM_CONNECT_TIMEOUT_MILLIS));
        }
        else {
            responseFlux = chatClient.prompt(prompt)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, agentId))
                    .stream()
                    .content()
                    // 3500ms未响应则判定超时，进行重连尝试
                    .timeout(Duration.ofMillis(ModelConstant.LLM_CONNECT_TIMEOUT_MILLIS));
        }

        return responseFlux
                .doOnSubscribe(callback::onSubscribe)
                .doFinally(callback::onFinish)
                .subscribe(
                        callback::onNext,
                        callback::onError
                );
    }

    /**
     * functionCall LLM 流式调用
     * @param result                FunctionCall从前端、嵌入式设备获取分析的结果
     * @param userQuestion          用户问题
     * @param chatClient            chatClient
     * @param agentId               聊天id
     * @param currentContextParam   上下文参数
     * @param callback              LLM回调
     * @return                      管理任务的Disposable
     */
    @Override
    public reactor.core.Disposable functionCallLLMStreamChat(
            @Nullable String result,
            @NonNull String userQuestion,
            @NonNull ChatClient chatClient,
            @NonNull String agentId,
            @NonNull LLMStateCallback callback
    ){
        if (result == null){
            result = "";
        }

        if (userQuestion.isEmpty()){
            callback.haveNoSentence();
            return null;
        }

        // todo 默认是vision，后续如果需要升级在修改。functionCall本质是调用前端获取结果。后续可以把所有的功能整合成枚举值，然后传递进入
        String systemPrompt = chatConfig.getVisionLLMPrompt(result);
        Prompt prompt = promptService.getChatPromptWhitSystemPrompt(
                userQuestion,
                systemPrompt
        );

        if (prompt == null){
            log.error("[functionCall LLM 提示词] 获取失败");
            throw new AppException(AgentExceptions.CHAT_CAN_NOT_BE_NULL);
        }

        log.info("[functionCall LLM 提示词] {}", prompt);

        Flux<String> responseFlux = chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, agentId))
                .stream()
                .content()
                // 3500ms未响应则判定超时，进行重连尝试
                .timeout(Duration.ofMillis(ModelConstant.LLM_CONNECT_TIMEOUT_MILLIS));

        return responseFlux
                .doOnSubscribe(callback::onSubscribe)
                .doFinally(callback::onFinish)
                .subscribe(
                        callback::onNext,
                        callback::onError
                );
    }

}
