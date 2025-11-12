package com.openapi;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/12 11:34
 */
public class STTLongTests {

    private static final String API_KEY = System.getenv("ALI_API_KEY");
    private static final String TTS_MODEL = "qwen3-tts-flash";

    // 不能超过600个字符
    private static final String text = """
            时维九月，序属三秋。潦水尽而寒潭清，烟光凝而暮山紫。俨骖騑于上路，访风景于崇阿。临帝子之长洲，得天人之旧馆。层峦耸翠，上出重霄；飞阁流丹，下临无地。鹤汀凫渚，穷岛屿之萦回；桂殿兰宫，即冈峦之体势。
            披绣闼，俯雕甍，山原旷其盈视，川泽纡其骇瞩。闾阎扑地，钟鸣鼎食之家；舸舰弥津，青雀黄龙之舳。云销雨霁，彩彻区明。落霞与孤鹜齐飞，秋水共长天一色。渔舟唱晚，响穷彭蠡之滨，雁阵惊寒，声断衡阳之浦。
            """;

    /**
     * 将 TTS 生成的音频数据转换为 STT 所需的 Flowable<ByteBuffer>
     */
    private static Flowable<ByteBuffer> convertAudioToFlowable(List<byte[]> audioChunks) {
        return Flowable.create(emitter -> {
            try {
                // 合并所有音频数据
                int totalLength = audioChunks.stream().mapToInt(chunk -> chunk.length).sum();
                ByteBuffer combinedBuffer = ByteBuffer.allocate(totalLength);

                for (byte[] chunk : audioChunks) {
                    combinedBuffer.put(chunk);
                }

                combinedBuffer.flip();

                // 不再分块，直接发送整个字节数组
                emitter.onNext(combinedBuffer);
                emitter.onComplete();

            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
    }

    private static final String STT_MODEL = "fun-asr-realtime";

    public static void sttStreamCall(Flowable<ByteBuffer> audioSource) throws NoApiKeyException {
        // 创建Recognizer
        Recognition recognizer = new Recognition();
        // 创建RecognitionParam，audioFrames参数中传入上面创建的Flowable<ByteBuffer>
        RecognitionParam sttParam = RecognitionParam.builder()
                .model(STT_MODEL)
                .format("pcm")
                .sampleRate(16000)
                // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
                .apiKey(API_KEY)
                .build();

        recognizer.streamCall(sttParam, audioSource)
                // 调用Flowable的subscribe方法订阅结果
                .blockingForEach(
                        result -> {
                            // 打印最终结果
                            if (result.isSentenceEnd()) {
                                System.out.println("Fix:" + result.getSentence().getText());
                            }
                            else {
                                System.out.println("Result:" + result.getSentence().getText());
                            }
                        });

    }

    private static void testCall() throws NoApiKeyException, InputRequiredException, UploadFileException {
        /// tts
        // 创建 TTS 流式请求
        MultiModalConversationParam ttsParam = MultiModalConversationParam.builder()
                // 仅支持qwen-tts系列模型，请勿使用除此之外的其他模型
                .model(TTS_MODEL)
//                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .apiKey(API_KEY)
                .text(text)
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();

        // 用于收集 TTS 生成的音频数据
        List<byte[]> audioChunks = new ArrayList<>();

        conv.streamCall(ttsParam, new ResultCallback<>() {
            @Override
            public void onEvent(MultiModalConversationResult result) {
                if (result != null && result.getOutput() != null
                        && result.getOutput().getAudio() != null){
                    String audioData = result.getOutput().getAudio().getData();
                    System.out.println("audioData length = " + (audioData == null ? 0 : audioData.length()));

                    // 将 base64 音频数据解码为字节数组并保存
                    if (audioData != null && !audioData.isEmpty()) {
                        byte[] audioBytes = Base64.getDecoder().decode(audioData);
                        audioChunks.add(audioBytes);
                    }
                }
            }

            @SneakyThrows
            @Override
            public void onComplete() {
                System.out.println("TTS 流式转换完成, 开始STT");

                /// stt
                Flowable<ByteBuffer> audioSource = convertAudioToFlowable(audioChunks);
                sttStreamCall(audioSource);
            }

            @Override
            public void onError(Exception e) {
                System.err.println("TTS 流式转换出错: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) throws NoApiKeyException, InputRequiredException, UploadFileException, InterruptedException {

        testCall();

        Thread.sleep(3 * 60_000L);
    }

}
