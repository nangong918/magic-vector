package com.magicvector.domain.dto.http.request;

import java.io.Serializable;

public class ChatByAnchorRequest implements Serializable {
    public String agentId;
    public Long anchorTimestamp;
    public Boolean before;
    public Integer limit;
}
