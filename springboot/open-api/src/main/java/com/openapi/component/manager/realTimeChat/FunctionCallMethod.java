package com.openapi.component.manager.realTimeChat;

import org.jetbrains.annotations.NotNull;

/**
 * @author 13225
 * @date 2025/11/7 19:32
 */
public interface FunctionCallMethod {

    void addFunctionCallResult(String result);
    @NotNull String getAllFunctionCallResult();
    void setIsFinalResultTTS(boolean isFinalResultTTS);

}
