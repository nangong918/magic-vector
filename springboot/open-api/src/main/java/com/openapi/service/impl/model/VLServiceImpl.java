package com.openapi.service.impl.model;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.google.gson.JsonElement;
import com.openapi.config.ChatConfig;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.service.model.VLService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author 13225
 * @date 2025/10/29 15:51
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class VLServiceImpl implements VLService {

    private final MultiModalConversation multiModalConversation;
    private final ChatConfig chatConfig;

    @NotNull
    @Override
    public String vlSingleFileBase64(@NotNull String base64Image, @NotNull String userQuestion) throws NoApiKeyException, UploadFileException {
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        new HashMap<>() {{
                            put("image", /*"data:image/png;base64," + */base64Image);
                        }},
                        new HashMap<>() {{
                            put("text", userQuestion);
                        }}
                )).build();

        var param = getVLParam(userMessage);

        MultiModalConversationResult result = multiModalConversation.call(param);
        return result.getOutput().getChoices()
                .getFirst()
                .getMessage().getContent()
                .getFirst()
                .get("text").toString();
    }

    @NotNull
    @Override
    public String vlListFileBase64(@NotNull List<String> base64ImageList, @NotNull String userQuestion) throws NoApiKeyException, UploadFileException {
        if (base64ImageList.isEmpty()){
            return "无图片数据";
        }
        if (base64ImageList.size() == 1){
            return vlSingleFileBase64(base64ImageList.getFirst(), userQuestion);
        }

        List<String> processedBase64ImageList = base64ImageList.stream()
                .map(it -> "data:image/jpeg;base64," + it)
                .toList();

        Map<String, Object> params = Map.of(
                "video", processedBase64ImageList,
                // 若模型属于Qwen2.5-VL系列且传入图像列表时，可设置fps参数，表示图像列表是由原视频每隔 1/fps 秒抽取的，其他模型设置则不生效
                "fps",2
        );
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(params,
                        Collections.singletonMap("text", userQuestion)))
                .build();

        var param = getVLParam(userMessage);

        MultiModalConversationResult result = multiModalConversation.call(param);
        return result.getOutput().getChoices()
                .getFirst()
                .getMessage().getContent()
                .getFirst()
                .get("text").toString();
    }

    @NotNull
    @Override
    public String vlVideoBase64(@NotNull String base64Video, @NotNull String userQuestion) throws NoApiKeyException, UploadFileException {
        if (base64Video.isEmpty()){
            return "无视频数据";
        }

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(new HashMap<>() {{
                                           put("video", "data:video/mp4;base64," + base64Video);// fps参数控制视频抽帧数量，表示每隔1/fps 秒抽取一帧
                                            // fps range: [0.1, 10]
                                           put("fps", ModelConstant.VISION_FPS);
                                       }},
                        new HashMap<>() {{
                            put("text", userQuestion);
                        }})).build();

        var param = getVLParam(userMessage);

        MultiModalConversationResult result = multiModalConversation.call(param);
        return result.getOutput().getChoices()
                .getFirst()
                .getMessage().getContent()
                .getFirst()
                .get("text").toString();
    }

    @NotNull
    private MultiModalConversationParam getVLParam(@NotNull MultiModalMessage userMessage){
        String systemPrompt = Optional.ofNullable(chatConfig.getVisionPrompt())
                .map(it -> it.get(ChatConfig.SYSTEM_PROMPT_KEY))
                .map(JsonElement::getAsString)
                .orElse("");
        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(
                        Collections.singletonMap("text", systemPrompt)))
                .build();
        return MultiModalConversationParam.builder()
                .apiKey(chatConfig.getApiKey())
                .model(ModelConstant.Vision_Model)
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();
    }
}
