package com.openapi.tts;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.openapi.MainApplication;
import com.openapi.domain.constant.ModelConstant;
import com.openapi.interfaces.model.TTSStateCallback;
import com.openapi.service.model.TTSServiceService;
import io.reactivex.disposables.Disposable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedList;
import java.util.List;

/**
 * @author 13225
 * @date 2025/11/12 16:13
 */
@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class TTSTests {
    private static final String text = """
            RK3566 被广泛应用于多种产品和行业，包括：
            智能家居设备: 例如智能音箱、智能显示屏等。
            工业自动化: 用于工控系统和无线监控设备。
            车载系统: 支持车载娱乐和导航系统。
            嵌入式系统: 各种终端设备，如POS机、数码标牌等。
            RK3566 是一款功能强大且灵活的处理器，适合多种嵌入式应用。通过高性能的 CPU 和 GPU、丰富的接口支持以及低功耗设计，RK3566 能够满足市场对性能和效率的需求。其广泛的应用场景使其成为开发者和制造商的理想选择。
            """;
    public static void splitSentence(@NonNull String sentence){
        List<String> splitSentence = new LinkedList<>();
        final int limit = 10;
        if (sentence.length() > limit) {
            // 讲句子拆分为ModelConstant.TTS_MaxLength等分
            for (int i = 0; i < sentence.length(); i += limit){
                splitSentence.add(
                        sentence.substring(i, Math.min(
                                i + limit, sentence.length()
                        ))
                );
            }
        }
        else {
            splitSentence.add(sentence);
        }

        for (String s : splitSentence){
            System.out.println(s);
        }
    }


    @Test
    void testSplitSentence(){
        splitSentence(text);
    }

    @Autowired
    private TTSServiceService ttsServiceService;

    @Test
    void ttsSelflyTest() throws InterruptedException {
        // 测试之前需要ModelConstant.TTS_MaxLength设置为50
        ttsServiceService.ttsSafelyStreamCall(text,
                new TTSStateCallback() {
                    @Override
                    public void recordDisposable(Disposable disposable) {

                    }

                    @Override
                    public void onStart(Subscription subscription) {
                        log.info("开始");
                    }

                    @Override
                    public void onNext(String audioBase64Data) {
                        log.info("数据长度: {}", audioBase64Data.length());
                    }

                    @Override
                    public void onSingleComplete() {
                        log.info("完成");
                    }

                    @Override
                    public void onAllComplete() {
                        log.info("所有完成");
                    }

                    @Override
                    public void haveNoSentence() {
                        log.info("没有句子");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.error("错误: {}", throwable.getMessage());
                    }
                });


        Thread.sleep(60_000);
    }

}
