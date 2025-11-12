package com.openapi.service.model;

import com.openapi.interfaces.mixLLM.STTCallback;
import io.reactivex.Flowable;

import java.nio.ByteBuffer;

/**
 * @author 13225
 * @date 2025/11/12 13:22
 */
public interface STTServiceService {
    void sttStreamCall(
            Flowable<ByteBuffer> audioSource,
            STTCallback sttCallback
    );
}
