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
     * 检测并从缓冲区提取N个完整的句子（解决tts请求频繁的问题）
     * @param textBuffer 流式文本缓冲区
     * @param needCount 需要提取的句子数量
     * @return 包含N个完整句子的字符串，如果不足N个则返回null
     */
    public String detectAndExtractNeedSentences(StringBuffer textBuffer, int needCount) {
        // 基础校验
        if (textBuffer == null || textBuffer.isEmpty() || needCount <= 0) {
            return null;
        }

        String currentText = textBuffer.toString();
        List<Term> termList = HanLP.segment(currentText);

        // 寻找第N个完整句子的结束位置
        int nthSentenceEndIndex = findNthSentenceEndIndex(termList, needCount);

        if (nthSentenceEndIndex > 0) {
            // 提取前N个完整句子
            String completeSentences = currentText.substring(0, nthSentenceEndIndex);
            // 从缓冲区移除已提取的句子
            textBuffer.delete(0, nthSentenceEndIndex);
            return completeSentences;
        }

        return null;
    }

    /**
     * 寻找第N个完整句子的结束索引
     */
    private int findNthSentenceEndIndex(List<Term> termList, int needCount) {
        if (termList.size() < 2) {
            return -1; // 至少需要两个词才能构成一个句子（内容+标点）
        }

        int currentPosition = 0;
        int sentenceCount = 0;

        // 遍历所有词，找到第N个符合条件的句末标点
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
                        sentenceCount++;

                        // 如果找到第N个句子，返回当前位置
                        if (sentenceCount == needCount) {
                            return currentPosition;
                        }
                    }
                }
            }
        }

        return -1; // 未找到足够数量的完整句子
    }

    /**
     * 获取缓冲区中完整的句子数量（用于判断是否达到N个句子）
     * @param textBuffer 文本缓冲区
     * @return 完整句子的数量
     */
    public int getCompleteSentenceCount(StringBuffer textBuffer) {
        if (textBuffer == null || textBuffer.isEmpty()) {
            return 0;
        }

        String currentText = textBuffer.toString();
        List<Term> termList = HanLP.segment(currentText);

        int sentenceCount = 0;

        for (int i = 0; i < termList.size(); i++) {
            Term currentTerm = termList.get(i);

            // 检查当前词是否为句末标点
            if (Nature.w.equals(currentTerm.nature) &&
                    END_PUNCTUATION.contains(currentTerm.word)) {

                // 确保标点前有有效内容
                if (i > 0) {
                    Term previousTerm = termList.get(i - 1);
                    if (!previousTerm.word.trim().isEmpty()) {
                        sentenceCount++;
                    }
                }
            }
        }

        return sentenceCount;
    }

    /**
     * 检查缓冲区中是否有至少N个完整句子
     * @param textBuffer 文本缓冲区
     * @param minCount 最小句子数量
     * @return 是否满足条件
     */
    public boolean hasEnoughSentences(StringBuffer textBuffer, int minCount) {
        return getCompleteSentenceCount(textBuffer) >= minCount;
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

    /**
     * 提取缓冲区中所有的完整句子
     * @param textBuffer 文本缓冲区
     * @return 包含所有完整句子的字符串，如果没有完整句子则返回null
     */
    public String extractAllCompleteSentences(StringBuffer textBuffer) {
        // 基础校验
        if (textBuffer == null || textBuffer.isEmpty()) {
            return null;
        }

        String currentText = textBuffer.toString();
        List<Term> termList = HanLP.segment(currentText);

        // 寻找最后一个完整句子的结束位置
        int lastSentenceEndIndex = findLastSentenceEndIndex(termList);

        if (lastSentenceEndIndex > 0) {
            // 提取所有完整句子
            String allCompleteSentences = currentText.substring(0, lastSentenceEndIndex);
            // 从缓冲区移除已提取的句子
            textBuffer.delete(0, lastSentenceEndIndex);
            return allCompleteSentences;
        }

        return null;
    }

    /**
     * 寻找最后一个完整句子的结束索引
     */
    private int findLastSentenceEndIndex(List<Term> termList) {
        if (termList.size() < 2) {
            return -1;
        }

        int currentPosition = 0;
        int lastValidEndIndex = -1;

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
                        // 更新最后一个有效结束位置
                        lastValidEndIndex = currentPosition;
                    }
                }
            }
        }

        return lastValidEndIndex;
    }

}
