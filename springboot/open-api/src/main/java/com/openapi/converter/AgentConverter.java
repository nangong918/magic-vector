package com.openapi.converter;

import com.openapi.domain.Do.AgentDo;
import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.vo.AgentVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.factory.Mappers;

/**
 * @author 13225
 * @date 2025/9/30 11:21
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AgentConverter {

    AgentConverter INSTANCE = Mappers.getMapper(AgentConverter.class);

    // do -> co
    @Mapping(source = "name", target = "name")
    @Mapping(source = "description", target = "description")
    AgentVo _do2Vo(AgentDo agentDo);

    // do -> vo
    default AgentVo do2Vo(AgentDo agentDo, String avatarUrl) {
        if (agentDo == null) {
            return null;
        }
        AgentVo vo = _do2Vo(agentDo);
        vo.setAvatarUrl(avatarUrl);
        return vo;
    }

    // do -> ao
    default AgentAo do2Ao(AgentDo agentDo){
        if (agentDo == null) {
            return null;
        }
        AgentAo ao = new AgentAo();
        AgentVo vo = _do2Vo(agentDo);
        ao.setAgentVo(vo);
        ao.setAgentId(agentDo.getId());
        ao.setUserId(agentDo.getUserId());
        return ao;
    }

    // do -> ao
    default AgentAo do2Ao(AgentDo agentDo, String avatarUrl){
        if (agentDo == null) {
            return null;
        }
        AgentAo ao = do2Ao(agentDo);
        ao.getAgentVo().setAvatarUrl(avatarUrl);
        return ao;
    }
}
