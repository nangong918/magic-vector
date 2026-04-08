package com.openapi.mapper;

import com.openapi.domain.Do.ControlAgentLogDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ControlAgentLogMapper {
    Integer insert(ControlAgentLogDo logDo);

    List<ControlAgentLogDo> queryByUserAgentPage(
            @Param("userId") Long userId,
            @Param("agentId") Long agentId,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );
}
