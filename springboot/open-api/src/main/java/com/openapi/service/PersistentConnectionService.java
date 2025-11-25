package com.openapi.service;


import com.alibaba.fastjson.JSONException;
import com.openapi.interfaces.connect.ConnectionSession;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 13225
 * @date 2025/11/13 17:44
 */
public interface PersistentConnectionService {

    void handleConnectMessage(
            @NotNull String connectMessage,
            @NotNull AtomicReference<String> agentId,
            @NotNull ConnectionSession connectionSession
    );

    void handleStartAudioRecordMessage(
            @NotNull String agentId
    );

    void handleStopAudioRecordMessage(
            @NotNull String agentId
    );

    void handleAudioChunk(
            @NotNull String base64Audio,
            @NotNull String agentId
    );

    void handleUserTextMessage(
            @NotNull String userTextMessage,
            @NotNull String agentId
    );

    void handleSystemMessage(
            @NotNull String systemMessage,
            @NotNull String agentId
    ) throws JSONException;
}
