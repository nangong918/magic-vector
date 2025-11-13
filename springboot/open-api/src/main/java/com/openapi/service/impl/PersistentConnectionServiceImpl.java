package com.openapi.service.impl;

import com.openapi.interfaces.connect.Message;
import com.openapi.service.PersistentConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author 13225
 * @date 2025/11/13 17:44
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistentConnectionServiceImpl implements PersistentConnectionService {
    @Override
    public void connect() {
        log.info("[PersistentConnection] connect");
    }

    @Override
    public void disconnect() {
        log.info("[PersistentConnection] disconnect");
    }

    @Override
    public void onThrowable(Throwable throwable) {

    }

    @Override
    public void onMessage(Message message) {

    }
}
