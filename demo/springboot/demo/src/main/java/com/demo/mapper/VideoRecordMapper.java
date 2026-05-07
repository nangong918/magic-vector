package com.demo.mapper;

import com.demo.domain.entity.VideoRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoRecordMapper {
    Integer insert(VideoRecordEntity entity);

    VideoRecordEntity selectByFileId(@Param("fileId") Long fileId);

    VideoRecordEntity selectByObjectName(@Param("objectName") String objectName);

    Integer updateByFileId(
            @Param("fileId") Long fileId,
            @Param("videoName") String videoName,
            @Param("fileSizeBytes") Long fileSizeBytes,
            @Param("durationSec") Double durationSec,
            @Param("bitrateKbps") Long bitrateKbps,
            @Param("objectName") String objectName,
            @Param("coverObjectName") String coverObjectName,
            @Param("hlsObjectName") String hlsObjectName,
            @Param("status") String status,
            @Param("errorMessage") String errorMessage,
            @Param("updatedAt") Long updatedAt
    );
}
