package com.demo.aarlib.voicewakeup;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class IvwResourceManager {
    private final Context appContext;
    private final String workDir;
    private final String resDir;

    IvwResourceManager(Context appContext, String configuredWorkDir) {
        this.appContext = appContext;
        this.workDir = resolveWorkDir(configuredWorkDir);
        this.resDir = this.workDir + "ivw";
    }

    String getWorkDir() {
        return workDir;
    }

    String getResDir() {
        return resDir;
    }

    void prepare() throws IOException {
        ensureWorkDirReady();
        syncIvwAssetsToWorkDir();
    }

    private String resolveWorkDir(String cfgDir) {
        if (!TextUtils.isEmpty(cfgDir)) {
            return cfgDir.endsWith(File.separator) ? cfgDir : cfgDir + File.separator;
        }
        File baseDir = appContext != null ? appContext.getExternalFilesDir(null) : null;
        if (baseDir == null && appContext != null) {
            baseDir = appContext.getFilesDir();
        }
        if (baseDir == null) {
            return File.separator;
        }
        String path = new File(baseDir, "iflytek").getAbsolutePath();
        return path.endsWith(File.separator) ? path : path + File.separator;
    }

    private void ensureWorkDirReady() throws IOException {
        File root = new File(workDir);
        File ivwDir = new File(resDir);
        File aikitDir = new File(workDir + "aikit");
        if (!root.exists() && !root.mkdirs()) {
            throw new IOException("创建workDir失败: " + workDir);
        }
        if (!ivwDir.exists() && !ivwDir.mkdirs()) {
            throw new IOException("创建ivw目录失败: " + resDir);
        }
        if (!aikitDir.exists() && !aikitDir.mkdirs()) {
            throw new IOException("创建aikit目录失败: " + aikitDir.getAbsolutePath());
        }
    }

    private void syncIvwAssetsToWorkDir() throws IOException {
        if (appContext == null) {
            throw new IOException("应用上下文为空，无法拷贝资源");
        }
        File ivwDir = new File(resDir);
        copyAssetFolder("ivw", ivwDir);
    }

    private void copyAssetFolder(String assetPath, File targetDir) throws IOException {
        String[] list = appContext.getAssets().list(assetPath);
        if (list == null) {
            return;
        }
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("创建资源目录失败: " + targetDir.getAbsolutePath());
        }
        for (String name : list) {
            String childAssetPath = assetPath + "/" + name;
            String[] childList = appContext.getAssets().list(childAssetPath);
            if (childList == null || childList.length == 0) {
                copyAssetFile(childAssetPath, new File(targetDir, name));
            } else {
                copyAssetFolder(childAssetPath, new File(targetDir, name));
            }
        }
    }

    private void copyAssetFile(String assetPath, File targetFile) throws IOException {
        try (InputStream in = appContext.getAssets().open(assetPath);
             FileOutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}
