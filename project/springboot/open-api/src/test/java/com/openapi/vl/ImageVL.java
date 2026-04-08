package com.openapi.vl;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import com.alibaba.dashscope.common.Role;


/**
 * @author 13225
 * @date 2025/10/29 9:58
 */
public class ImageVL {
    // 若使用新加坡地域的模型，请取消下列注释
    // static {Constants.baseHttpApiUrl="https://dashscope-intl.aliyuncs.com/api/v1";}
    public static void callWithLocalFile(String localPath)
            throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(new HashMap<String, Object>(){{put("image", localPath);}},
                        new HashMap<String, Object>(){{put("text", "图中描绘的是什么景象？");}})).build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                // 新加坡和北京地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                .apiKey(System.getenv("ALI_API_KEY"))
                .model("qwen3-vl-flash")  // 此处以qwen3-vl-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/models
                .messages(Collections.singletonList(userMessage))
                .build();
        MultiModalConversationResult result = conv.call(param);
        System.out.println("AI文件方式回复" + result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));}

    public static void callWithLocalFileBase64(String localPath) throws ApiException, NoApiKeyException, UploadFileException, IOException {

        String base64Image = encodeImageToBase64(localPath); // Base64编码

        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        new HashMap<>() {{
                            put("image", "data:image/png;base64," + base64Image);
                        }},
                        new HashMap<>() {{
                            put("text", "图中描绘的是什么景象？");
                        }}
                )).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 新加坡和北京地域的API Key不同。获取API Key：https://help.aliyun.com/zh/model-studio/get-api-key
                .apiKey(System.getenv("ALI_API_KEY"))
                .model("qwen3-vl-flash")
                .messages(Collections.singletonList(userMessage))
                .build();

        MultiModalConversationResult result = conv.call(param);
        System.out.println("AI Base64方式回复" + result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
    }


    public static void main(String[] args) throws IOException {
        String localPath = "C:/Users/13225/Pictures/to.jpg";
        String base64Image = encodeImageToBase64(localPath);
        // 407688 -> 大约305.8 KB（原先298kb）
        System.out.println("base64Image length: " + base64Image.length());
//        System.out.println(base64Image);

        try {
            // 将xxx/eagle.png替换为你本地图像的绝对路径
            callWithLocalFile(localPath);
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            System.out.println(e.getMessage());
        }

        try {
            // 将 xxx/eagle.png 替换为你本地图像的绝对路径
            callWithLocalFileBase64(localPath);
        } catch (ApiException | NoApiKeyException | UploadFileException | IOException e) {
            System.out.println(e.getMessage());
        }

        System.exit(0);
    }

    public static String encodeImageToBase64(String imagePath) throws IOException {
        Path path = Paths.get(imagePath);
        byte[] imageBytes = Files.readAllBytes(path);
        return Base64.getEncoder().encodeToString(imageBytes);
    }

}
