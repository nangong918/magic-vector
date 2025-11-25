package com.openapi.vl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
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
 * @date 2025/10/29 11:03
 */
public class VideoVL {


    private static final String MODEL_NAME = "qwen3-vl-flash";
    private static final String APL_API_KEY = System.getenv("ALI_API_KEY");

    public static void callWithLocalFile(String localPath)
            throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(new HashMap<String, Object>()
                                       {{
                                           put("video", localPath);// fps参数控制视频抽帧数量，表示每隔1/fps 秒抽取一帧
                                           put("fps", 0.1);
                                       }},
                        new HashMap<String, Object>(){{put("text", "这段视频描绘的是什么景象？");}})).build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 新加坡和北京地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                .apiKey(APL_API_KEY)
                .model(MODEL_NAME)
                .messages(Collections.singletonList(userMessage))
                .build();
        MultiModalConversationResult result = conv.call(param);
        System.out.println(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
    }

    private static String encodeVideoToBase64(String videoPath) throws IOException {
        Path path = Paths.get(videoPath);
        byte[] videoBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(videoBytes);
    }

    public static void callWithLocalFileBase64(String localPath)
            throws ApiException, NoApiKeyException, UploadFileException, IOException {

        String base64Video = encodeVideoToBase64(localPath); // Base64编码

        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(new HashMap<>() {{
                                           put("video", "data:video/mp4;base64," + base64Video);// fps参数控制视频抽帧数量，表示每隔1/fps 秒抽取一帧
                                           put("fps", 0.1);
                                       }},
                        new HashMap<>() {{
                            put("text", "这段视频描绘的是什么景象？");
                        }})).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                // 新加坡和北京地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                .apiKey(APL_API_KEY)
                .model(MODEL_NAME)
                .messages(Collections.singletonList(userMessage))
                .build();

        MultiModalConversationResult result = conv.call(param);
        System.out.println(result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
    }

    public static void main(String[] args) {
        String localPath = "D:\\Bandicam\\test.mp4";
        try {
            // 将xxxx/test.mp4替换为你本地视频的绝对路径
            callWithLocalFile(localPath);
            callWithLocalFileBase64(localPath);
        } catch (ApiException | NoApiKeyException | UploadFileException | IOException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }

}
