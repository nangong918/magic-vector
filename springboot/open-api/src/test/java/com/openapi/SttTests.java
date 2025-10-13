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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class SttTests {

    private static final String API_KEY = System.getenv("ALI_API_KEY");

    public static void testStream() throws NoApiKeyException{
        // 创建一个Flowable<ByteBuffer>
        Flowable<ByteBuffer> audioSource = Flowable.create(emitter -> {
                    new Thread(() -> {
                        try {
                            // 创建音频格式
                            AudioFormat audioFormat = new AudioFormat(16000, 16, 1, true, false);
                            // 根据格式匹配默认录音设备
                            TargetDataLine targetDataLine =
                                    AudioSystem.getTargetDataLine(audioFormat);
                            targetDataLine.open(audioFormat);
                            // 开始录音
                            targetDataLine.start();
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            long start = System.currentTimeMillis();
                            // 录音3s并进行实时转写
                            while (System.currentTimeMillis() - start < 3_000) {
                                int read = targetDataLine.read(buffer.array(), 0, buffer.capacity());
                                if (read > 0) {
                                    buffer.limit(read);
                                    // 将录音音频数据发送给流式识别服务
                                    emitter.onNext(buffer);
                                    buffer = ByteBuffer.allocate(1024);
                                    // 录音速率有限，防止cpu占用过高，休眠一小会儿
                                    Thread.sleep(20);
                                }
                            }
                            // 通知结束转写
                            System.out.println("结束录音");
                            emitter.onComplete();
                        } catch (Exception e) {
                            emitter.onError(e);
                        }
                    }).start();
                },
                BackpressureStrategy.BUFFER);

        // 创建Recognizer
        Recognition recognizer = new Recognition();
        // 创建RecognitionParam，audioFrames参数中传入上面创建的Flowable<ByteBuffer>
        RecognitionParam param = RecognitionParam.builder()
                .model("fun-asr-realtime")
                .format("pcm")
                .sampleRate(16000)
                // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
                .apiKey(API_KEY)
                .build();

        // 流式调用接口
        System.out.println("流式调用接口");
        recognizer.streamCall(param, audioSource)
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

        try {
            System.out.println("等待15秒");
            Thread.sleep(15000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.exit(0);
    }

    private static final String ttsModel = "qwen3-tts-flash";

    public static void testCall() throws NoApiKeyException, UploadFileException, InputRequiredException {
        /// tts
        // 创建 TTS 流式请求
        MultiModalConversationParam ttsParam = MultiModalConversationParam.builder()
                // 仅支持qwen-tts系列模型，请勿使用除此之外的其他模型
                .model(ttsModel)
//                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .apiKey(API_KEY)
                .text("你好啊，我是小小lemon酱")
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
                    System.out.println("audioData = " + audioData);

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

                // 模拟实时音频流，分块发送
                int chunkSize = 3200; // 200ms 的音频数据 (16000采样率 * 2字节 * 0.2秒)
                while (combinedBuffer.remaining() > 0) {
                    int currentChunkSize = Math.min(chunkSize, combinedBuffer.remaining());
                    byte[] chunkArray = new byte[currentChunkSize];
                    combinedBuffer.get(chunkArray);

                    ByteBuffer chunkBuffer = ByteBuffer.wrap(chunkArray);
                    emitter.onNext(chunkBuffer);

                    // 模拟实时音频流的延迟
                    Thread.sleep(10);
                }

                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
    }

    public static void sttStreamCall(Flowable<ByteBuffer> audioSource) throws NoApiKeyException {
        // 创建Recognizer
        Recognition recognizer = new Recognition();
        // 创建RecognitionParam，audioFrames参数中传入上面创建的Flowable<ByteBuffer>
        RecognitionParam sttParam = RecognitionParam.builder()
                .model("fun-asr-realtime")
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

        try {
            System.out.println("等待10秒");
            Thread.sleep(10_000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws NoApiKeyException, UploadFileException, InterruptedException, InputRequiredException {

        System.out.println("api-key: " + API_KEY);

        testCall();

        // 等待流式处理完成
        Thread.sleep(10_000L);
    }
}