package com.demo.aarlib.voicewakeup;

import android.text.TextUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

final class WakeKeywordFileWriter {

    private WakeKeywordFileWriter() {
    }

    static void writeKeywordFile(String resDir, String keywordInput) throws IOException {
        if (TextUtils.isEmpty(keywordInput)) {
            throw new IllegalArgumentException("唤醒词不能为空");
        }
        File dir = new File(resDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("创建资源目录失败: " + resDir);
        }
        File keywordFile = new File(resDir + "/keyword.txt");
        if (keywordFile.exists() && !keywordFile.delete()) {
            throw new IOException("删除旧keyword.txt失败: " + keywordFile.getAbsolutePath());
        }
        File binFile = new File(resDir + "/keyword.bin");
        if (binFile.exists() && !binFile.delete()) {
            throw new IOException("删除旧keyword.bin失败: " + binFile.getAbsolutePath());
        }
        String normalized = keywordInput.replace("，", ",");
        String[] keywords = normalized.split(",");
        if (!keywordFile.exists() && !keywordFile.createNewFile()) {
            throw new IOException("创建keyword.txt失败: " + keywordFile.getAbsolutePath());
        }
        int validCount = 0;
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(keywordFile), StandardCharsets.UTF_8);
             BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
            for (String item : keywords) {
                String kw = item.trim();
                if (!kw.isEmpty()) {
                    bufferedWriter.write(kw);
                    bufferedWriter.write(";");
                    bufferedWriter.newLine();
                    validCount++;
                }
            }
        }
        if (validCount == 0) {
            throw new IllegalArgumentException("唤醒词不能为空");
        }
    }
}
