package com.openapi;


import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.JsonUtils;
import com.openapi.config.ChatConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
    public void streamTtsToFileTest() throws Exception {
        // 创建 TTS 流式请求
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .apiKey(config.getApiKey())
                .text("你好啊，我是小小lemon酱，这是一个语音测试")
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();

        // 用于收集所有的音频数据块
        List<String> audioDataChunks = new ArrayList<>();
        final String[] outputFileName = {null};

        conv.streamCall(param, new ResultCallback<MultiModalConversationResult>() {
            @Override
            public void onEvent(MultiModalConversationResult result) {
                if (result != null && result.getOutput() != null
                        && result.getOutput().getAudio() != null) {

                    String audioData = result.getOutput().getAudio().getData();
                    String audioUrl = result.getOutput().getAudio().getUrl();

                    System.out.println("收到音频事件:");
                    System.out.println("Audio URL: " + audioUrl);
                    System.out.println("Audio Data length: " + (audioData != null ? audioData.length() : 0));

                    if (audioData != null && !audioData.isEmpty()) {
                        // 收集音频数据块
                        audioDataChunks.add(audioData);
                        System.out.println("已收集 " + audioDataChunks.size() + " 个音频数据块");

                        // 如果是第一个数据块，创建文件并立即保存测试
                        if (audioDataChunks.size() == 1) {
                            try {
                                String testFileName = "first_chunk_test.wav";
                                saveBase64ToWavFile(audioData, testFileName);
                                System.out.println("第一个数据块已保存为: " + testFileName);
                            } catch (Exception e) {
                                System.err.println("保存第一个数据块失败: " + e.getMessage());
                            }
                        }
                    }
                }
            }

            @Override
            public void onComplete() {
                System.out.println("TTS 流式转换完成");
                System.out.println("总共收集到 " + audioDataChunks.size() + " 个音频数据块");

                // 合并所有数据块并保存为完整文件
                if (!audioDataChunks.isEmpty()) {
                    try {
                        String finalFileName = "complete_audio_" + System.currentTimeMillis() + ".wav";
                        saveAllChunksToWavFile(audioDataChunks, finalFileName);
                        outputFileName[0] = finalFileName;
                        System.out.println("完整音频已保存为: " + finalFileName);
                    } catch (Exception e) {
                        System.err.println("保存完整音频失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                System.err.println("TTS 流式转换出错: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // 等待流式处理完成
        Thread.sleep(15000L);

        if (outputFileName[0] != null) {
            System.out.println("测试完成！请检查文件: " + outputFileName[0]);
        } else {
            System.out.println("测试完成，但未生成音频文件");
        }
    }

    /**
     * 将单个Base64数据块保存为WAV文件
     */
    private void saveBase64ToWavFile(String base64Data, String fileName) throws Exception {
        try {
            // Base64解码
            byte[] audioBytes = Base64.getDecoder().decode(base64Data);
            System.out.println("解码后音频数据大小: " + audioBytes.length + " bytes");

            // 保存为文件
            Path filePath = Paths.get(fileName);
            Files.write(filePath, audioBytes);
            System.out.println("文件已保存: " + filePath.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("保存Base64数据到文件失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 合并所有数据块并保存为WAV文件
     */
    private void saveAllChunksToWavFile(List<String> base64Chunks, String fileName) throws Exception {
        try {
            // 合并所有Base64数据块
            StringBuilder combinedBase64 = new StringBuilder();
            for (String chunk : base64Chunks) {
                combinedBase64.append(chunk);
            }

            // Base64解码
            byte[] audioBytes = Base64.getDecoder().decode(combinedBase64.toString());
            System.out.println("合并后音频数据总大小: " + audioBytes.length + " bytes");

            // 保存为文件
            Path filePath = Paths.get(fileName);
            Files.write(filePath, audioBytes);
            System.out.println("完整音频文件已保存: " + filePath.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("合并保存音频文件失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 另一种测试方法：逐个保存每个数据块
     */
    @Test
    public void streamTtsSaveChunksTest() throws Exception {
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .apiKey(config.getApiKey())
                .text("你好啊，这是分块保存测试")
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();
        final int[] chunkCount = {0};

        conv.streamCall(param, new ResultCallback<MultiModalConversationResult>() {
            @Override
            public void onEvent(MultiModalConversationResult result) {
                if (result != null && result.getOutput() != null
                        && result.getOutput().getAudio() != null) {

                    String audioData = result.getOutput().getAudio().getData();

                    if (audioData != null && !audioData.isEmpty()) {
                        chunkCount[0]++;
                        try {
                            String chunkFileName = "audio_chunk_" + chunkCount[0] + ".wav";
                            saveBase64ToWavFile(audioData, chunkFileName);
                            System.out.println("第 " + chunkCount[0] + " 个数据块已保存为: " + chunkFileName);
                        } catch (Exception e) {
                            System.err.println("保存数据块 " + chunkCount[0] + " 失败: " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onComplete() {
                System.out.println("TTS 流式转换完成，共保存 " + chunkCount[0] + " 个数据块");
            }

            @Override
            public void onError(Exception e) {
                System.err.println("TTS 流式转换出错: " + e.getMessage());
            }
        });

        Thread.sleep(15000L);
    }
}
