package com.openapi;


import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioSpeechApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class AudioTest {

    @Autowired
    private SpeechSynthesisModel speechSynthesisModel;

    private static final String FILE_PATH = "D:\\audio\\simple_tts_output.mp3";

    @Test
    public void ttsTest(){
        String text = "你好啊，我是一个AI模型";

        SpeechSynthesisPrompt speechSynthesisPrompt = new SpeechSynthesisPrompt(text);

        SpeechSynthesisResponse speechSynthesisResponse = speechSynthesisModel.call(
                speechSynthesisPrompt
        );

        ByteBuffer byteBuffer = speechSynthesisResponse.getResult().getOutput().getAudio();
        byte[] bytes = byteBuffer.array();

        File file = new File(FILE_PATH);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            log.info("保存文件成功 -->  filePath = {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("调用 simpleTTS 保存到文件异常 -->  userInputPrompt ={}, e", text, e);
        }
    }


    @Autowired
    private DashScopeAudioSpeechModel dashScopeAudioSpeechModel;


    // yml配置无效，准备改用直接使用dashscope-sdk
    @Test
    public void tts2Test(){

        String text = "你好啊，我叫小Q";

        SpeechSynthesisPrompt speechSynthesisPrompt = new SpeechSynthesisPrompt(text);

        SpeechSynthesisResponse speechSynthesisResponse = dashScopeAudioSpeechModel.call(speechSynthesisPrompt);

        ByteBuffer byteBuffer = speechSynthesisResponse.getResult().getOutput().getAudio();
        byte[] bytes = byteBuffer.array();

        File file = new File(FILE_PATH);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            log.info("保存文件成功 -->  filePath = {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("调用 simpleTTS 保存到文件异常 -->  userInputPrompt ={}, e", text, e);
        }
    }
}
