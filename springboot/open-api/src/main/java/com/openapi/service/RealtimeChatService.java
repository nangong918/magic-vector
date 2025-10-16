package com.openapi.service;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.openapi.component.manager.RealtimeChatContextManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/10/16 10:08
 */
public interface RealtimeChatService {
    void startChat(@NotNull RealtimeChatContextManager chatContextManager) throws InterruptedException, NoApiKeyException;
}
