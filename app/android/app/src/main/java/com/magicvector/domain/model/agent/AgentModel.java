package com.magicvector.domain.model.agent;

import com.data.domain.vo.agent.AgentVo;
import com.magicvector.utils.sort.SortItem;
import com.magicvector.utils.sort.SortMode;

import org.jetbrains.annotations.NotNull;


/**
 * @author 13225
 * @date 2025/9/30 11:25
 */
public class AgentModel implements SortItem {
    public AgentVo agentVo;
    public String agentId;
    public String userId;
    public Long lastChatTime = 0L;

    @Override
    public long getUid() {
        return Long.parseLong(agentId);
    }

    @Override
    public long getTimestamp() {
        return lastChatTime;
    }

    @Override
    public @NotNull String getStringIndex() {
        return agentVo.name;
    }

    @Override
    public @NotNull Comparable<?> getSortValue(@NotNull SortMode mode) {
        // agent页面默认用name排序
        return getStringIndex();
    }
}
