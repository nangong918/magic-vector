package com.openapi.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.openapi.component.manager.RealtimeChatContextManager;
import com.openapi.domain.exception.AppException;
import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;

import java.io.IOException;

/**
 * @author 13225
 * @date 2025/10/16 10:08
 */
public interface RealtimeChatService {
    io.reactivex.disposables.Disposable startAudioChat(@NotNull RealtimeChatContextManager chatContextManager) throws InterruptedException, NoApiKeyException;

    ChatClient initChatClient(@NotNull RealtimeChatContextManager chatContextManager, @NotNull DashScopeChatModel chatModel) throws AppException;

    void startTextChat(@NotNull String userQuestion, @NotNull RealtimeChatContextManager chatContextManager) throws AppException;

    /**
     * 开启视觉聊天
     * @param imageBase64            图片的base64
     * @param chatContextManager     聊天上下文管理器
     * @param isPassiveNotActive     是被动调用
     * @throws NoApiKeyException     没有API Key
     * @throws UploadFileException   上传文件异常
     */
    void startVisionChat(@Nullable String imageBase64, @NotNull RealtimeChatContextManager chatContextManager, boolean isPassiveNotActive) throws NoApiKeyException, UploadFileException;
}
