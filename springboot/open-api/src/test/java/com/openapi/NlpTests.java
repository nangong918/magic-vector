package com.openapi;

import com.openapi.component.manager.OptimizedSentenceDetector;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author 13225
 * @date 2025/10/24 11:02
 */
@Slf4j
@SpringBootTest(classes = MainApplication.class)
public class NlpTests {

    @Autowired
    private OptimizedSentenceDetector optimizedSentenceDetector;


    private final String text1 = "今天天气不错，ollen今天去哪里1？";
    private final String text2 = "想你了。今天天气不错，ollen今天去哪里1？";
    private final String text3 = "想你了。今天天气不错，ollen今天去哪里1？哈哈哈";
    private final String text4 = "昨天跟骞卉打了4小时31分钟的电话。今天上班一直都心不在焉。不知道是因为睡得太晚，还是因为她对我说的话。昨天做梦也梦见了这些事情。";
    private final String text5 = "这个女生很奇怪，总是希望把自己的全部展现出去看看会不会把对方吓走。可能是以前遇到人都只是图她外表不真诚。其实任何人都不是十全十美的。而她自由意志，勇敢与坚强很打动我，因为我也有相同炽热的灵魂。就连我们做过的事，所做所想都很像。她还是ENTP，虽然没有ENFP那样的情绪价值，但是拥有ENFP没有的超有趣灵魂。哈哈哈";


    @Test
    public void getSentenceTest() {

        StringBuffer textBuffer = new StringBuffer(text1);
        String sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 3);
        log.info("sentence1: {}", sentence);
        log.info("textBuffer1: {}", textBuffer);

        textBuffer = new StringBuffer(text2);
        sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 3);
        log.info("sentence2: {}", sentence);
        log.info("textBuffer2: {}", textBuffer);

        textBuffer = new StringBuffer(text3);
        sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 3);
        log.info("sentence3: {}", sentence);
        log.info("textBuffer3: {}", textBuffer);

        textBuffer = new StringBuffer(text4);
        sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 3);
        log.info("sentence4: {}", sentence);
        log.info("textBuffer4: {}", textBuffer);

        textBuffer = new StringBuffer(text5);
        sentence = optimizedSentenceDetector.detectAndExtractNeedSentences(textBuffer, 3);
        log.info("sentence5: {}", sentence);
        log.info("textBuffer5: {}", textBuffer);
        sentence = optimizedSentenceDetector.extractAllCompleteSentences(textBuffer);
        log.info("sentence5-all: {}", sentence);
        log.info("textBuffer5-all: {}", textBuffer);
    }

}
