package com.openapi.service.impl;

import com.minio.domain.ao.SuccessFile;
import com.minio.service.OssService;
import com.openapi.config.AgentConfig;
import com.openapi.converter.AgentConverter;
import com.openapi.domain.Do.AgentDo;
import com.openapi.domain.ao.AgentAo;
import com.openapi.mapper.AgentMapper;
import com.openapi.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Service
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;
    private final OssService ossService;
    private final AgentConfig agentConfig;

    @Override
    public AgentAo createAgent(@Nullable MultipartFile avatar, @NotNull String userId, @NotNull String name, @NotNull String description) {
        AgentDo agentDo = new AgentDo();
        agentDo.setName(name);
        agentDo.setUserId(userId);
        agentDo.setDescription(description);

        if (avatar == null){
            agentMapper.insert(agentDo);
            return agentConverter.do2Ao(agentDo);
        }

        val files = List.of(avatar);
        val result = ossService.uploadFiles(
                files,
                agentDo.getId(),
                agentConfig.getBucketName()
        );
        String ossId = Optional.ofNullable(result.getSuccessFiles())
                        .filter(list -> !list.isEmpty())
                        .map(List::getFirst)
                        .map(SuccessFile::getFileId)
                        .orElse(null);
        agentDo.setOssId(ossId);
        agentMapper.insert(agentDo);

        if (ossId == null){
            return agentConverter.do2Ao(agentDo);
        }

        val fileIds = List.of(ossId);
        val avatarUrls = ossService.getFileUrlsByFileIds(fileIds);

        String avatarUrl = Optional.of(avatarUrls)
                        .filter(list -> !list.isEmpty())
                        .map(List::getFirst)
                        .orElse(null);
        return agentConverter.do2Ao(agentDo, avatarUrl);
    }

    @Override
    public AgentAo getAgentById(String id) {
        AgentDo agentDo = agentMapper.selectById(id);
        if (agentDo == null || agentDo.getId() == null){
            return null;
        }
        if (agentDo.getOssId() == null){
            return agentConverter.do2Ao(agentDo);
        }
        val fileIds = List.of(agentDo.getOssId());
        val avatarUrls = ossService.getFileUrlsByFileIds(fileIds);
        String avatarUrl = Optional.of(avatarUrls)
                        .filter(list -> !list.isEmpty())
                        .map(List::getFirst)
                        .orElse(null);
        return agentConverter.do2Ao(agentDo, avatarUrl);
    }
}
