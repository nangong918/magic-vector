package com.magicvector.domain.dto.http.request;

import java.io.Serializable;

public class AgentDeleteRequest implements Serializable {
    private String agentId;
    private String userId;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
