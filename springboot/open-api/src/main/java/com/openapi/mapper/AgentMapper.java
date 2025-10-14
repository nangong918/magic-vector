package com.openapi.mapper;

import com.openapi.domain.Do.AgentDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 13225
 * @date 2025/9/30 10:59
 */
@Mapper
public interface AgentMapper {

    /// 增
    Integer insert(AgentDo agentDo);

    Integer insertBatch(@Param("list") List<AgentDo> agentDos);

    /// 删
    Integer deleteById(String id);

    Integer deleteByIds(@Param("list") List<String> ids);

    /// 改
    Integer update(AgentDo agentDo);

    Integer updateBatch(@Param("list") List<AgentDo> agentDos);

    /// 查
    AgentDo selectById(String id);

    List<AgentDo> selectByIds(@Param("list") List<String> ids);

    List<String> selectAllIds();

}
