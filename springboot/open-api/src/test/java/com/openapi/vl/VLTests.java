package com.openapi.vl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.openapi.MainApplication;
import com.openapi.config.ChatConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Qwen VL 视觉理解测试
 * 参考网址：<a href="https://help.aliyun.com/zh/model-studio/vision?spm=a2c4g.11186623.help-menu-2400256.d_0_2_0.24886be9vqRmce&scm=20140722.H_2845871._.OR_help-T_cn~zh-V_1#071b239d9371c">...</a>
 */
@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class VLTests {

    @Autowired
    private ChatConfig chatConfig;

    @Test
    public void readPromptTest(){
        // 读取JSON文件
        try (InputStream inputStream = getClass().getResourceAsStream("/ai/visionPrompt.json")) {
            if (inputStream == null){
                System.err.println("JSON文件不存在");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                // 使用Gson解析JSON
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                // 获取systemPrompt字段
                String systemPrompt = jsonObject.get("systemPrompt").getAsString();

                System.out.println("systemPrompt: " + systemPrompt);
                System.out.println("提示词长度: " + systemPrompt.length() + "字");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        String systemPrompt = chatConfig.getVisionPrompt().get("systemPrompt").getAsString();
        System.out.println("systemPrompt: " + systemPrompt);
    }

}
