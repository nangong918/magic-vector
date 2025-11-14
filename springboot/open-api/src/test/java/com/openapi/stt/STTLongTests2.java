package com.openapi.stt;

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
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author 13225
 * @date 2025/11/12 12:32
 */
public class STTLongTests2 {

    private static final String API_KEY = System.getenv("ALI_API_KEY");
    private static final String TTS_MODEL = "qwen3-tts-flash";
    private static final String STT_MODEL = "paraformer-realtime-v2";

    // 不能超过600个字符
    private static final String text = """
            RK3566 被广泛应用于多种产品和行业，包括：
            智能家居设备: 例如智能音箱、智能显示屏等。
            工业自动化: 用于工控系统和无线监控设备。
            车载系统: 支持车载娱乐和导航系统。
            嵌入式系统: 各种终端设备，如POS机、数码标牌等。
            RK3566 是一款功能强大且灵活的处理器，适合多种嵌入式应用。通过高性能的 CPU 和 GPU、丰富的接口支持以及低功耗设计，RK3566 能够满足市场对性能和效率的需求。其广泛的应用场景使其成为开发者和制造商的理想选择。
            """;

    public static void main(String[] args) {
        try {
            // 检查 API Key
            if (API_KEY == null || API_KEY.trim().isEmpty()) {
                System.err.println("错误: 请设置 ALI_API_KEY 环境变量");
                return;
            }

            System.out.println("开始流式 TTS -> STT 测试...");
//            testStreamCall();

            testStreamCall2();

        } catch (Exception e) {
            System.err.println("程序执行出错: " + e.getMessage());
        }
    }

    private static void testStreamCall() throws NoApiKeyException, InputRequiredException, UploadFileException {
        // 创建用于TTS音频数据流转发的处理器
        PublishProcessor<ByteBuffer> audioStreamProcessor = PublishProcessor.create();

        // 创建STT识别结果的字符串构建器
        AtomicReference<StringBuilder> sttResultBuilder = new AtomicReference<>(new StringBuilder());
        CountDownLatch completionLatch = new CountDownLatch(1);

        // 先启动STT识别
        startSTTRecognition(audioStreamProcessor, sttResultBuilder, completionLatch);

        // 然后启动TTS转换，将音频数据实时转发给STT
        startTTSToSTTStream(audioStreamProcessor, completionLatch);
    }

    private static void testStreamCall2() throws NoApiKeyException, InputRequiredException, UploadFileException, InterruptedException {
        // 创建用于TTS音频数据流转发的处理器
        PublishProcessor<ByteBuffer> audioStreamProcessor = PublishProcessor.create();

        // 创建STT识别结果的字符串构建器
        AtomicReference<StringBuilder> sttResultBuilder = new AtomicReference<>(new StringBuilder());

        // 先启动STT识别
        startSTTRecognition2(audioStreamProcessor, sttResultBuilder);

        // 然后启动TTS转换，将音频数据实时转发给STT
        startTTSToSTTStream2(audioStreamProcessor);

        Thread.sleep(120_000L);
    }

    /**
     * 启动STT识别
     */
    private static void startSTTRecognition(Flowable<ByteBuffer> audioSource,
                                            AtomicReference<StringBuilder> resultBuilder,
                                            CountDownLatch completionLatch) {
        new Thread(() -> {
            try {
                Recognition recognizer = new Recognition();

                RecognitionParam sttParam = RecognitionParam.builder()
                        .model(STT_MODEL)
                        .format("pcm")
                        .sampleRate(16000)
                        .apiKey(API_KEY)
                        .build();

                var disposable = recognizer.streamCall(sttParam, audioSource)
                        .subscribe(
                                result -> {
                                    handleRecognitionResult(result, resultBuilder);
                                },
                                error -> {
                                    System.err.println("STT 识别出错: " + error.getMessage());
                                    completionLatch.countDown();
                                },
                                () -> {
                                    System.out.println("\nSTT 识别完成");
                                    String finalResult = resultBuilder.get().toString();
                                    System.out.println("最终识别结果: " + finalResult);
                                    completionLatch.countDown();
                                }
                        );

            } catch (Exception e) {
                System.err.println("STT 调用异常: " + e.getMessage());
                completionLatch.countDown();
            }
        }).start();
    }

    /**
     * 启动STT识别
     */
    private static void startSTTRecognition2(Flowable<ByteBuffer> audioSource,
                                            AtomicReference<StringBuilder> resultBuilder) {
        try {
            Recognition recognizer = new Recognition();

            RecognitionParam sttParam = RecognitionParam.builder()
                    .model(STT_MODEL)
                    .format("pcm")
                    .sampleRate(16000)
                    .apiKey(API_KEY)
                    .build();

            var disposable = recognizer.streamCall(sttParam, audioSource)
                    .subscribe(
                            result -> {
                                handleRecognitionResult(result, resultBuilder);
                            },
                            error -> {
                                System.err.println("STT 识别出错: " + error.getMessage());
                            },
                            () -> {
                                System.out.println("\nSTT 识别完成");
                                String finalResult = resultBuilder.get().toString();
                                System.out.println("最终识别结果: " + finalResult);
                            }
                    );

        } catch (Exception e) {
            System.err.println("STT 调用异常: " + e.getMessage());
        }
    }

    /**
     * 启动TTS转换并实时转发音频数据到STT
     */
    private static void startTTSToSTTStream(PublishProcessor<ByteBuffer> audioStreamProcessor,
                                            CountDownLatch completionLatch) throws NoApiKeyException, InputRequiredException, UploadFileException {
        MultiModalConversationParam ttsParam = MultiModalConversationParam.builder()
                .model(TTS_MODEL)
                .apiKey(API_KEY)
                .text(text)
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();

        conv.streamCall(ttsParam, new ResultCallback<>() {
            @Override
            public void onEvent(MultiModalConversationResult result) {
                if (result != null && result.getOutput() != null
                        && result.getOutput().getAudio() != null) {

                    String audioData = result.getOutput().getAudio().getData();

                    if (audioData != null && !audioData.isEmpty()) {
                        try {
                            byte[] audioBytes = Base64.getDecoder().decode(audioData);
                            System.out.println("收到 TTS 音频数据块, 大小: " + audioBytes.length + " bytes");

                            // 立即将音频数据转发给STT识别
                            ByteBuffer audioBuffer = ByteBuffer.wrap(audioBytes);
                            audioStreamProcessor.onNext(audioBuffer);

                        } catch (Exception e) {
                            System.err.println("解码音频数据失败: " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onComplete() {
                System.out.println("TTS 转换完成，发送结束信号给STT");
                // 通知STT音频流结束
                audioStreamProcessor.onComplete();
            }

            @Override
            public void onError(Exception e) {
                System.err.println("TTS 流式转换出错: " + e.getMessage());
                audioStreamProcessor.onError(e);
                completionLatch.countDown();
            }
        });

        // 等待处理完成
        try {
            if (!completionLatch.await(120, TimeUnit.SECONDS)) {
                System.out.println("处理超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("处理被中断");
        }
    }

    /**
     * 启动TTS转换并实时转发音频数据到STT
     */
    private static void startTTSToSTTStream2(PublishProcessor<ByteBuffer> audioStreamProcessor) throws NoApiKeyException, InputRequiredException, UploadFileException {
        MultiModalConversationParam ttsParam = MultiModalConversationParam.builder()
                .model(TTS_MODEL)
                .apiKey(API_KEY)
                .text(text)
                .voice(AudioParameters.Voice.CHERRY)
                .languageType("Chinese")
                .build();

        MultiModalConversation conv = new MultiModalConversation();

        conv.streamCall(ttsParam, new ResultCallback<>() {
            @Override
            public void onEvent(MultiModalConversationResult result) {
                if (result != null && result.getOutput() != null
                        && result.getOutput().getAudio() != null) {

                    String audioData = result.getOutput().getAudio().getData();

                    if (audioData != null && !audioData.isEmpty()) {
                        try {
                            byte[] audioBytes = Base64.getDecoder().decode(audioData);
                            System.out.println("收到 TTS 音频数据块, 大小: " + audioBytes.length + " bytes");

                            // 立即将音频数据转发给STT识别
                            ByteBuffer audioBuffer = ByteBuffer.wrap(audioBytes);
                            audioStreamProcessor.onNext(audioBuffer);

                        } catch (Exception e) {
                            System.err.println("解码音频数据失败: " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void onComplete() {
                System.out.println("TTS 转换完成，发送结束信号给STT");
                // 通知STT音频流结束
                audioStreamProcessor.onComplete();
            }

            @Override
            public void onError(Exception e) {
                System.err.println("TTS 流式转换出错: " + e.getMessage());
                audioStreamProcessor.onError(e);
            }
        });

    }

    /**
     * 处理STT识别结果
     */
    private static void handleRecognitionResult(RecognitionResult result,
                                                AtomicReference<StringBuilder> resultBuilder) {
        if (result == null) {
            return;
        }

        try {
            if (result.getSentence() != null && result.getSentence().getText() != null) {
                String text = result.getSentence().getText();

                if (result.isSentenceEnd()) {
                    // 最终结果
                    resultBuilder.get().append(text).append(" ");
                    System.out.println("\n🔴 最终结果: " + text);
                    System.out.print("当前完整文本: " + resultBuilder.get().toString());
                } else {
                    // 中间结果
                    System.out.println("\n🟡 中间结果: " + text);
                }
            }
        } catch (Exception e) {
            System.err.println("处理识别结果时出错: " + e.getMessage());
        }
    }
}