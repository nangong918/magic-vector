package com.openapi.service.impl;

import com.minio.domain.ao.SuccessFile;
import com.minio.service.OssService;
import com.openapi.config.AgentConfig;
import com.openapi.converter.AgentConverter;
import com.openapi.domain.Do.AgentDo;
import com.openapi.domain.Do.ChatMessageDo;
import com.openapi.domain.ao.AgentAo;
import com.openapi.domain.ao.AgentChatAo;
import com.openapi.mapper.AgentMapper;
import com.openapi.service.AgentService;
import com.openapi.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
@Service
public class AgentServiceImpl implements AgentService {

    private final AgentMapper agentMapper;
    private final AgentConverter agentConverter;
    private final OssService ossService;
    private final AgentConfig agentConfig;
    private final ChatMessageService chatMessageService;

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

    @NotNull
    @Override
    public List<AgentAo> getAgentsByIds(List<String> ids){
        if (CollectionUtils.isEmpty(ids)){
            return new ArrayList<>();
        }
        List<AgentDo> agentDos = agentMapper.selectByIds(ids);
        if (CollectionUtils.isEmpty(agentDos)){
            return new ArrayList<>();
        }
        List<String> fileIds = agentDos.stream().map(AgentDo::getOssId).toList();
        List<String> avatarUrls = ossService.getFileUrlsByFileIds(fileIds);

        assert avatarUrls.size() == agentDos.size();
        List<AgentAo> agentAos = new ArrayList<>();
        for (int i = 0; i < agentDos.size(); i++){
            agentAos.add(agentConverter.do2Ao(agentDos.get(i), avatarUrls.get(i)));
        }
        return agentAos;
    }

    @NotNull
    @Override
    public List<String> getUserAgents(String userId){
        List<String> agentIds = new ArrayList<>();
        if (!StringUtils.hasText(userId)){
            return agentIds;
        }
        agentIds = agentMapper.selectAllByUserId(userId);
        return agentIds;
    }

    @NotNull
    @Override
    public List<AgentAo> getUserAgentsAo(String userId){
        List<String> agentIds =getUserAgents(userId);
        if (agentIds.isEmpty()){
            return new ArrayList<>();
        }
        return getAgentsByIds(agentIds);
    }

    /**
     * 获取用户与Agent最近Chat的Ao
     * @param userId    用户Id
     * @return          聊天aoList
     */
    @NotNull
    @Override
    public List<AgentChatAo> getLastAgentChatList(@NotNull String userId){
        List<String> agentIds = getUserAgents(userId);
        if (agentIds.isEmpty()){
            return new ArrayList<>();
        }

        List<AgentAo> agentAos = getAgentsByIds(agentIds);
        if (agentAos.isEmpty()){
            return new ArrayList<>();
        }

        // 填充agentAo
        List<AgentChatAo> agentChatAos = new ArrayList<>(agentAos.size());
        for (AgentAo agentAo : agentAos){
            var agentChatAo = new AgentChatAo();
            agentChatAo.setAgentAo(agentAo);
            agentChatAos.add(agentChatAo);
        }

        // 获取最新20条消息
        List<List<ChatMessageDo>> chatMessageDos = chatMessageService.getLast20MessagesByAgentIds(agentIds);

        // 使用 Map 来提高性能
        Map<String, List<ChatMessageDo>> chatMessagesMap = new HashMap<>();
        for (int i = 0; i < agentChatAos.size(); i++) {
            String agentId = agentChatAos.get(i).getAgentAo().getAgentId();
            chatMessagesMap.put(agentId, chatMessageDos.get(i));
        }

        // 填充最新20条消息并获取最后时间
        for (AgentChatAo agentChatAo : agentChatAos) {
            String agentId = agentChatAo.getAgentAo().getAgentId();
            List<ChatMessageDo> messages = chatMessagesMap.get(agentId);
            if (messages != null) {
                agentChatAo.setLastChatMessages(messages);

                // 获取最后时间
                LocalDateTime lastChatTime = messages.stream()
                        .map(ChatMessageDo::getChatTime)
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.MIN);

                // 设置最后时间
                agentChatAo.setLastChatTime(lastChatTime);
            }
        }

        return agentChatAos;
    }
}
