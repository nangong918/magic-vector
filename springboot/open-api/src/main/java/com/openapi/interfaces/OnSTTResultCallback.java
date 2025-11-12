package com.openapi.interfaces;

import com.alibaba.dashscope.exception.NoApiKeyException;

import java.io.IOException;

/**
 * @author 13225
 * @date 2025/10/13 17:23
 */
public interface OnSTTResultCallback {
    void onResult(String result) throws NoApiKeyException, IOException;
}
