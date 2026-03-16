package com.openapi.mapper;

import com.openapi.domain.Do.VideoRecordDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoRecordMapper {
    Integer insert(VideoRecordDo videoRecordDo);

    Integer update(VideoRecordDo videoRecordDo);

    VideoRecordDo getById(@Param("id") Long id);

    List<VideoRecordDo> queryByUserId(
            @Param("userId") Long userId,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );
}
