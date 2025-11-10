package com.openapi.interfaces.model;

import lombok.NonNull;

/**
 * @author 13225
 * @date 2025/11/10 10:31
 */
public interface LLMErrorCallback {
    /**
     * 增加错误次数并检查是否超出重试限制 (上游内部需要进行原子的自增并检查)
     * @return  int[2] {是否超出?1:0, 重试次数}
     */
    int @NonNull [] addCountAndCheckIsOverLimit();

    /**
     * 将重试任务的disposable进行管理
     * @param task  任务
     */
    void addTask(Object task);

    /**
     * 停止会话(上游需要控制将超出限制重置)
     */
    void endConversation();
}
