package com.openapi.vl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Collections;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;


/**
 * @author 13225
 * @date 2025/10/29 10:23
 */
public class ImageListVL {

    private static final String MODEL_NAME = "qwen3-vl-flash";  // 此处以qwen3-vl-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/models
    public static void videoImageListSample(String localPath1, String localPath2, String localPath3, String localPath4)
            throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        Map<String, Object> params = Map.of(
                "video", Arrays.asList(localPath1, localPath2, localPath3, localPath4),
                // 若模型属于Qwen2.5-VL系列且传入图像列表时，可设置fps参数，表示图像列表是由原视频每隔 1/fps 秒抽取的，其他模型设置则不生效
                "fps",2);
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(params,
                        Collections.singletonMap("text", "描述这个视频的具体过程")))
                .build();
        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(
                        Collections.singletonMap("text", "你是一个助手，请根据视频内容进行回答。回答尽量精简，告诉大概内容就可以。")))
                .build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 新加坡和北京地域的API Key不同。获取API Key：https://www.alibabacloud.com/help/zh/model-studio/get-api-key
                .apiKey(System.getenv("ALI_API_KEY"))
                .model(MODEL_NAME)
                .messages(Arrays.asList(systemMessage, userMessage)).build();
        MultiModalConversationResult result = conv.call(param);
        System.out.print(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
    }

    private static String encodeImageToBase64(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static void videoImageListSampleBase64(String localPath1,String localPath2, String localPath3, String localPath4)
            throws ApiException, NoApiKeyException, UploadFileException, IOException {

        String base64Image1 = encodeImageToBase64(localPath1); // Base64编码
        String base64Image2 = encodeImageToBase64(localPath2);
        String base64Image3 = encodeImageToBase64(localPath3);
        String base64Image4 = encodeImageToBase64(localPath4);


        MultiModalConversation conv = new MultiModalConversation();
        Map<String, Object> params = Map.of(
                "video", Arrays.asList(
                        "data:image/jpeg;base64," + base64Image1,
                        "data:image/jpeg;base64," + base64Image2,
                        "data:image/jpeg;base64," + base64Image3,
                        "data:image/jpeg;base64," + base64Image4),
                // 若模型属于Qwen2.5-VL系列且传入图像列表时，可设置fps参数，表示图像列表是由原视频每隔 1/fps 秒抽取的，其他模型设置则不生效
                "fps",2
        );
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(params,
                        Collections.singletonMap("text", "描述这个视频的具体过程")))
                .build();
        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(
                        Collections.singletonMap("text", "你是一个助手，请根据视频内容进行回答。回答尽量精简，告诉大概内容就可以。")))
                .build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 新加坡和北京地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                .apiKey(System.getenv("ALI_API_KEY"))
                .model(MODEL_NAME)
                .messages(Arrays.asList(systemMessage, userMessage))
                .build();

        MultiModalConversationResult result = conv.call(param);
        System.out.println(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
    }

    public static void main(String[] args) {
        String localPath1 = "C:/Users/13225/Pictures/APP选色1.png";
        String localPath2 = "C:/Users/13225/Pictures/APP选色2.png";
        String localPath3 = "C:/Users/13225/Pictures/APP选色1.png";
        String localPath4 = "C:/Users/13225/Pictures/APP选色2.png";
        try {
            videoImageListSample(
                    localPath1,
                    localPath2,
                    localPath3,
                    localPath4
            );
            videoImageListSampleBase64(
                    localPath1,
                    localPath2,
                    localPath3,
                    localPath4
            );
        } catch (ApiException | NoApiKeyException | UploadFileException | IOException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }

}
