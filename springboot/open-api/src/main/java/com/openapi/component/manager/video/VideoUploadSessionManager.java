package com.openapi.component.manager.video;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VideoUploadSessionManager {
    private final Map<String, UploadSession> sessionMap = new ConcurrentHashMap<>();

    public UploadSession createSession(Long userId, String fileName, Long fileSize, Integer chunkSize) {
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        File tempFile = new File(System.getProperty("java.io.tmpdir"), "video-upload-" + uploadId + ".tmp");
        UploadSession session = new UploadSession();
        session.setUploadId(uploadId);
        session.setUserId(userId);
        session.setFileName(fileName);
        session.setFileSize(fileSize == null ? 0L : fileSize);
        session.setChunkSize(chunkSize == null ? 5 * 1024 * 1024 : chunkSize);
        session.setUploadedOffset(0L);
        session.setTempFile(tempFile);
        sessionMap.put(uploadId, session);
        return session;
    }

    public UploadSession getSession(String uploadId) {
        return sessionMap.get(uploadId);
    }

    public void removeSession(String uploadId) {
        sessionMap.remove(uploadId);
    }

    @Data
    public static class UploadSession {
        private String uploadId;
        private Long userId;
        private String fileName;
        private Long fileSize;
        private Integer chunkSize;
        private Long uploadedOffset;
        private File tempFile;
    }
}
