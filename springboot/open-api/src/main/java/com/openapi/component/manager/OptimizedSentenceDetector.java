package com.openapi.component.manager;


import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.seg.common.Term;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OptimizedSentenceDetector {

    /**
     * 句末标点集合
     */
    public static final String END_PUNCTUATION = "。？！.!?";

    /**
     * 检测并提取缓冲区中的第一个完整句子
     * @param textBuffer 流式文本缓冲区
     * @return 第一个完整句子，如果没有则返回null
     */
    public String detectAndExtractFirstSentence(StringBuffer textBuffer) {
        // 基础校验
        if (textBuffer == null || textBuffer.isEmpty()) {
            return null;
        }

        String currentText = textBuffer.toString();
        List<Term> termList = HanLP.segment(currentText);

        // 遍历分词结果，寻找第一个完整句子的结束位置
        int sentenceEndIndex = findFirstSentenceEndIndex(termList);

        if (sentenceEndIndex > 0) {
            // 提取完整句子
            String completeSentence = currentText.substring(0, sentenceEndIndex);
            // 从缓冲区移除已提取的句子
            textBuffer.delete(0, sentenceEndIndex);
            return completeSentence;
        }

        return null;
    }

    /**
     * 寻找第一个完整句子的结束索引
     */
    private int findFirstSentenceEndIndex(List<Term> termList) {
        if (termList.size() < 2) {
            return -1; // 至少需要两个词才能构成一个句子（内容+标点）
        }

        int currentPosition = 0;

        // 遍历所有词，找到第一个符合条件的句末标点
        for (int i = 0; i < termList.size(); i++) {
            Term currentTerm = termList.get(i);
            currentPosition += currentTerm.word.length();

            // 检查当前词是否为句末标点
            if (Nature.w.equals(currentTerm.nature) &&
                    END_PUNCTUATION.contains(currentTerm.word)) {

                // 确保标点前有有效内容
                if (i > 0) {
                    Term previousTerm = termList.get(i - 1);
                    if (!previousTerm.word.trim().isEmpty()) {
                        // 找到第一个完整句子的结束位置
                        return currentPosition;
                    }
                }
            }
        }

        return -1; // 未找到完整句子
    }

}
