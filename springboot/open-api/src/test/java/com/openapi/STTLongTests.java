package com.openapi;

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author 13225
 * @date 2025/11/12 11:34
 */
public class STTLongTests {

    private static final String API_KEY = System.getenv("ALI_API_KEY");
    private static final String TTS_MODEL = "qwen3-tts-flash";

    // 不能超过600个字符
    private static final String text = """
            RK3566 被广泛应用于多种产品和行业，包括：
            智能家居设备: 例如智能音箱、智能显示屏等。
            工业自动化: 用于工控系统和无线监控设备。
            车载系统: 支持车载娱乐和导航系统。
            嵌入式系统: 各种终端设备，如POS机、数码标牌等。
            RK3566 是一款功能强大且灵活的处理器，适合多种嵌入式应用。通过高性能的 CPU 和 GPU、丰富的接口支持以及低功耗设计，RK3566 能够满足市场对性能和效率的需求。其广泛的应用场景使其成为开发者和制造商的理想选择。
            """;

    /**
     * 改进的音频数据转换方法 - 模拟流式传输
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

                // 模拟流式传输：将数据分成小块发送
                int chunkSize = 3200; // 200ms 的数据 (16000Hz * 16bit * 0.2s / 8 = 3200 bytes)
                byte[] audioData = combinedBuffer.array();

                int position = 0;
                while (position < audioData.length && !emitter.isCancelled()) {
                    int end = Math.min(position + chunkSize, audioData.length);
                    byte[] chunk = new byte[end - position];
                    System.arraycopy(audioData, position, chunk, 0, chunk.length);

                    ByteBuffer chunkBuffer = ByteBuffer.wrap(chunk);
                    emitter.onNext(chunkBuffer);

                    position = end;

                    // 添加小延迟模拟实时流
                    if (position < audioData.length) {
                        Thread.sleep(50); // 50ms 延迟
                    }
                }

                emitter.onComplete();

            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
    }

    private static final String STT_MODEL = "paraformer-realtime-v2"; // 建议使用这个模型

    public static void sttStreamCall(Flowable<ByteBuffer> audioSource) throws NoApiKeyException {
        // 创建Recognizer
        Recognition recognizer = new Recognition();

        // 创建RecognitionParam，配置正确的参数
        RecognitionParam sttParam = RecognitionParam.builder()
                .model(STT_MODEL)
                .format("pcm")
                .sampleRate(16000)
                // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
                .apiKey(API_KEY)
                .build();

        try {
            CountDownLatch completionLatch = new CountDownLatch(1);

            recognizer.streamCall(sttParam, audioSource)
                    .subscribe(
                            result -> {
                                handleRecognitionResult(result);
                            },
                            error -> {
                                System.err.println("STT 识别出错: " + error.getMessage());
                                error.printStackTrace();
                                completionLatch.countDown();
                            },
                            () -> {
                                System.out.println("STT 识别完成");
                                completionLatch.countDown();
                            }
                    );

            // 等待识别完成，设置超时时间
            if (!completionLatch.await(30, TimeUnit.SECONDS)) {
                System.out.println("STT 识别超时");
            }

        } catch (Exception e) {
            System.err.println("STT 调用异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理识别结果
     */
    private static void handleRecognitionResult(RecognitionResult result) {
        if (result == null) {
            return;
        }

        try {
            if (result.isSentenceEnd()) {
                System.out.println("最终结果: " + result.getSentence().getText());
            } else {
                System.out.println("中间结果: " + result.getSentence().getText());
            }
        } catch (Exception e) {
            System.err.println("处理识别结果时出错: " + e.getMessage());
        }
    }

    private static void testCall() throws NoApiKeyException, InputRequiredException, UploadFileException {
        // 用于收集 TTS 生成的音频数据
        List<byte[]> audioChunks = new ArrayList<>();
        CountDownLatch ttsLatch = new CountDownLatch(1);

        /// tts
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

        conv.streamCall(ttsParam, new ResultCallback<MultiModalConversationResult>() {
            @Override
            public void onEvent(MultiModalConversationResult result) {
                if (result != null && result.getOutput() != null
                        && result.getOutput().getAudio() != null) {
                    String audioData = result.getOutput().getAudio().getData();

                    if (audioData != null && !audioData.isEmpty()) {
                        try {
                            byte[] audioBytes = Base64.getDecoder().decode(audioData);
                            audioChunks.add(audioBytes);
                            System.out.println("收到 TTS 音频数据块, 大小: " + audioBytes.length + " bytes");
                        } catch (Exception e) {
                            System.err.println("解码音频数据失败: " + e.getMessage());
                        }
                    }
                }
            }

            @SneakyThrows
            @Override
            public void onComplete() {
                System.out.println("TTS 流式转换完成, 总共收集 " + audioChunks.size() + " 个音频块");
                System.out.println("开始 STT 识别...");

                try {
                    /// stt
                    Flowable<ByteBuffer> audioSource = convertAudioToFlowable(audioChunks);
                    sttStreamCall(audioSource);
                } catch (Exception e) {
                    System.err.println("STT 处理失败: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    ttsLatch.countDown();
                }
            }

            @Override
            public void onError(Exception e) {
                System.err.println("TTS 流式转换出错: " + e.getMessage());
                e.printStackTrace();
                ttsLatch.countDown();
            }
        });

        try {
            // 等待 TTS 完成
            if (!ttsLatch.await(60, TimeUnit.SECONDS)) {
                System.out.println("TTS 处理超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("等待 TTS 完成时被中断");
        }
    }

    public static void main(String[] args) {
        try {
            // 检查 API Key
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                System.err.println("错误: 请设置 ALI_API_KEY 环境变量");
                return;
            }

            System.out.println("开始 TTS -> STT 测试...");
            testCall();

        } catch (Exception e) {
            System.err.println("程序执行出错: " + e.getMessage());
        }
    }
}