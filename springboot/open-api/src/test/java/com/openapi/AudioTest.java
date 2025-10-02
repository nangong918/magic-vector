package com.openapi;


import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.JsonUtils;
import com.openapi.component.manager.OptimizedSentenceDetector;
import com.openapi.config.ChatConfig;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

    @Autowired
    private ChatConfig config;

    private static final String MODEL = "qwen3-tts-flash";
    @Test
    public void dashScopeTtsTest() throws Exception{
        System.out.println("ali_key = " + config.getApiKey());

        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 仅支持qwen-tts系列模型，请勿使用除此之外的其他模型
                .model(MODEL)
//                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .apiKey(config.getApiKey())
                .text("你好啊，我是小小lemon酱")
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversationResult result = conv.call(param);

        System.out.println(JsonUtils.toJson(result));
    }

    @Test
    public void streamTtsTest() throws Exception{
        // 创建 TTS 流式请求
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 仅支持qwen-tts系列模型，请勿使用除此之外的其他模型
                .model(MODEL)
//                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .apiKey(config.getApiKey())
                .text("你好啊，我是小小lemon酱")
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();

        conv.streamCall(param, new ResultCallback<>() {
            @Override
            public void onEvent(MultiModalConversationResult result) {
                // 处理流式返回的音频数据
                if (result != null && result.getOutput() != null
                        && result.getOutput().getAudio() != null) {

                    String audioUrl = result.getOutput().getAudio().getUrl();
                    String audioData = result.getOutput().getAudio().getData();

                    System.out.println("收到音频事件:");
                    System.out.println("Audio URL: " + audioUrl);
                    System.out.println("Audio Data length: " +
                            audioData);

                    // 如果有音频数据，可以实时播放或处理
                    if (!audioData.isEmpty()) {
                        // 实时播放音频数据
//                        playAudioData(audioData);
                        System.out.println("播放音频数据中...: audioData.length: " + audioData);
                    }
                }
            }

            @Override
            public void onComplete() {
                System.out.println("TTS 流式转换完成");
            }

            @Override
            public void onError(Exception e) {
                System.err.println("TTS 流式转换出错: " + e.getMessage());
            }
        });

        // 等待流式处理完成
        Thread.sleep(10000L);
    }

    @Test
    public void streamTtsTest2() throws Exception{
        // 创建 TTS 流式请求
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 仅支持qwen-tts系列模型，请勿使用除此之外的其他模型
                .model(MODEL)
//                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .apiKey(config.getApiKey())
                .text("你好啊，我是小小lemon酱")
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();

        Flowable<MultiModalConversationResult> result = conv.streamCall(param);

        result.blockingForEach(r -> {System.out.println(JsonUtils.toJson(r));
        });

        // 等待流式处理完成
        Thread.sleep(10000L);
    }

    // 失败，存储的音频无法播放
    @Test
    public void streamTtsTest3() throws Exception {
        // 创建 TTS 流式请求
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .apiKey(config.getApiKey())
                .text("你好啊，我是小小lemon酱，这是语音测试")
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();

        Flowable<MultiModalConversationResult> result = conv.streamCall(param);

        // 用于收集音频数据
        List<byte[]> audioChunks = new ArrayList<>();
        final int[] chunkCount = {0};

        result.blockingForEach(r -> {
            System.out.println("收到响应: " + JsonUtils.toJson(r));

            // 检查是否有音频数据
            if (r != null && r.getOutput() != null && r.getOutput().getAudio() != null) {
                String audioData = r.getOutput().getAudio().getData();
                String audioUrl = r.getOutput().getAudio().getUrl();

                System.out.println("Audio URL: " + audioUrl);
                System.out.println("Audio Data present: " + (audioData != null && !audioData.isEmpty()));

                // 处理Base64音频数据
                if (audioData != null && !audioData.isEmpty()) {
                    try {
                        // Base64解码
                        byte[] audioBytes = Base64.getDecoder().decode(audioData);
                        chunkCount[0]++;

                        System.out.println("第 " + chunkCount[0] + " 个音频数据块，大小: " + audioBytes.length + " bytes");

                        // 保存这个数据块到文件
                        String chunkFileName = "audio_chunk_" + chunkCount[0] + ".wav";
                        Files.write(Paths.get(chunkFileName), audioBytes);
                        System.out.println("已保存: " + chunkFileName);

                        // 同时收集到列表中用于合并
                        audioChunks.add(audioBytes);

                    } catch (Exception e) {
                        System.err.println("处理音频数据失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        });

        // 合并所有音频数据块
        if (!audioChunks.isEmpty()) {
            try {
                // 计算总大小
                int totalSize = audioChunks.stream().mapToInt(chunk -> chunk.length).sum();
                System.out.println("准备合并 " + audioChunks.size() + " 个数据块，总大小: " + totalSize + " bytes");

                // 合并所有字节数组
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(totalSize);
                for (byte[] chunk : audioChunks) {
                    outputStream.write(chunk);
                }

                byte[] combinedAudio = outputStream.toByteArray();

                // 保存合并后的文件
                String finalFileName = "combined_audio_" + System.currentTimeMillis() + ".wav";
                Files.write(Paths.get(finalFileName), combinedAudio);
                System.out.println("合并音频已保存: " + finalFileName + ", 大小: " + combinedAudio.length + " bytes");

            } catch (Exception e) {
                System.err.println("合并音频文件失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("处理完成，共处理 " + chunkCount[0] + " 个音频数据块");
    }

    // 成功，因为模型生成的不是Wav的Base64，而是PCM
    @Test
    public void streamTtsTest4() throws Exception {
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .apiKey(config.getApiKey())
                .text("你好啊，我是小小lemon酱")
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();
        Flowable<MultiModalConversationResult> result = conv.streamCall(param);

        result.blockingForEach(r -> {
            try {
                String base64Data = r.getOutput().getAudio().getData();
                if (base64Data != null && !base64Data.isEmpty()) {
                    byte[] audioBytes = Base64.getDecoder().decode(base64Data);

                    // 添加调试信息
                    System.out.println("收到音频数据: " + audioBytes.length + " 字节");

                    // 音频格式配置（保持官方推荐参数）
                    AudioFormat format = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            24000,
                            16,
                            1,
                            2,
                            16000,
                            false
                    );

                    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

                    // 检查音频线路是否可用
                    if (!AudioSystem.isLineSupported(info)) {
                        System.err.println("音频线路不支持，尝试其他格式...");
                        // 尝试常见格式
                        format = new AudioFormat(24000, 16, 1, true, false);
                    }

                    try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
                        line.open(format);
                        line.start();
                        line.write(audioBytes, 0, audioBytes.length);
                        line.drain();
                        line.close();
                        System.out.println("音频播放完成");
                    }
                }
            } catch (LineUnavailableException e) {
                System.err.println("音频线路不可用: " + e.getMessage());

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(10000L);
    }

    // 简单的测试方法验证音频系统
    @Test
    public void testAudioSystem() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        System.out.println("可用的音频设备:");
        for (Mixer.Info mixer : mixers) {
            System.out.println(" - " + mixer.getName() + " : " + mixer.getDescription());
        }
    }

    @Test
    public void streamTtsTest5() throws Exception {
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .apiKey(config.getApiKey())
                .text("你好啊，我是小小lemon酱")
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();
        Flowable<MultiModalConversationResult> result = conv.streamCall(param);

        // 在循环外初始化音频线路
        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                24000,
                16,
                1,
                2,
                16000,
                false
        );

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("音频线路不支持，尝试其他格式...");
            format = new AudioFormat(24000, 16, 1, true, false);
            info = new DataLine.Info(SourceDataLine.class, format);
        }

        // 创建并打开音频线路（只执行一次）
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        try {
            result.blockingForEach(r -> {
                try {
                    String base64Data = r.getOutput().getAudio().getData();
                    if (base64Data != null && !base64Data.isEmpty()) {
                        byte[] audioBytes = Base64.getDecoder().decode(base64Data);

                        // 添加调试信息
                        System.out.println("收到音频数据: " + audioBytes.length + " 字节");

                        // 直接写入数据，不关闭线路
                        line.write(audioBytes, 0, audioBytes.length);
                        System.out.println("音频数据已写入");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            // 所有数据发送完成后等待播放完毕
            line.drain();

        } finally {
            // 最终关闭音频线路
            line.stop();
            line.close();
        }
    }

    public void playSoundByText(@NotNull String text,
                                @NotNull SourceDataLine line,
                                @NotNull MultiModalConversation conv) throws Exception{
        if (text.isEmpty()){
            return;
        }

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .apiKey(config.getApiKey())
                .text(text)
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        var result = conv.streamCall(param);

        result.blockingForEach(r -> {
            String base64Data = r.getOutput().getAudio().getData();
            if (base64Data != null && !base64Data.isEmpty()) {
                byte[] audioBytes = Base64.getDecoder().decode(base64Data);

                // 添加调试信息
                System.out.println("收到音频数据: " + audioBytes.length + " 字节");

                // 直接写入数据，不关闭线路
                line.write(audioBytes, 0, audioBytes.length);
                System.out.println("音频数据已写入");
            }
        });

        // 所有数据发送完成后等待播放完毕
        line.drain();
    }


    @Autowired
    DashScopeChatModel dashScopeChatModel;

    @Autowired
    OptimizedSentenceDetector optimizedSentenceDetector;

    @Test
    public void streamTtsChatTest() throws Exception {

        /// 音频模型
        MultiModalConversation conv = new MultiModalConversation();
        // 在循环外初始化音频线路
        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                24000,
                16,
                1,
                2,
                16000,
                false
        );

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("音频线路不支持，尝试其他格式...");
            format = new AudioFormat(24000, 16, 1, true, false);
            info = new DataLine.Info(SourceDataLine.class, format);
        }

        // 创建并打开音频线路（只执行一次）
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        /// 聊天模型
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel)
                .build();

        String systemPrompt = "你只能输出自然语言，不要输出表情等特殊符号。你聊天需要按照正常的中文标准在结尾输出结束标点符号，例如：" + OptimizedSentenceDetector.END_PUNCTUATION;
        String userQuestion = "你好啊，你是谁？介绍一下自己";

        // 获取流式响应
        Flux<String> responseFlux = chatClient.prompt(systemPrompt)
                .user(userQuestion)
                .stream()
                .content();

        StringBuffer textBuffer = new StringBuffer();

        // 订阅流式响应并处理
        responseFlux.subscribe(
                // 处理每个流片段
                fragment -> {
                    // 将新片段添加到缓冲区
                    textBuffer.append(fragment);
                    System.out.println("\n[接收到片段]: " + fragment);

                    // 尝试从缓冲区提取完整句子并输出
                    String completeSentence;
                    while ((completeSentence = optimizedSentenceDetector.detectAndExtractFirstSentence(textBuffer)) != null) {
                        System.out.println("\n[***提取到完整句子***]: " + completeSentence);
                        // 在这里可以调用TTS服务生成音频
                        try {
                            playSoundByText(completeSentence, line, conv);
                        } catch (Exception e) {
                            System.err.println("TTS服务调用错误: " + e.getMessage());
                        }
                    }

                    // 显示当前缓冲区剩余内容
                    if (!textBuffer.isEmpty()) {
                        System.out.println("[缓冲区剩余]: " + textBuffer);
                    }
                },
                // 处理错误
                error -> System.err.println("流式处理错误: " + error.getMessage()),
                // 处理完成
                () -> {
                    System.out.println("\n[流式响应结束]");
                    // 处理缓冲区中可能剩余的不完整内容
                    if (!textBuffer.isEmpty()) {
                        System.out.println("[最终剩余未完成内容]: " + textBuffer);
                    }
                }
        );

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 最终关闭音频线路
            line.stop();
            line.close();
        }
    }

}
