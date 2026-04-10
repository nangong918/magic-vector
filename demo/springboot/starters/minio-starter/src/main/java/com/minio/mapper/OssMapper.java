package com.minio.mapper;

import com.minio.domain.entity.OssEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 13225
 * @date 2025/4/17 18:14
 */
@Mapper
public interface OssMapper {

    Integer insert(OssEntity ossEntity);

    OssEntity getById(Long id);

    List<OssEntity> getByIds(@Param("list") List<Long> ids);

    OssEntity getByIdempotentKey(@Param("idempotentKey") String idempotentKey);

    List<OssEntity> queryByUserIdOrderByCreatedAt(
            @Param("userId") Long userId,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    List<OssEntity> queryByUserIdOrderByFileSize(
            @Param("userId") Long userId,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    List<OssEntity> queryByUserIdAll(@Param("userId") Long userId);

    Integer updateOriginFileNameById(
            @Param("id") Long id,
            @Param("originFileName") String originFileName,
            @Param("updatedAt") Long updatedAt
    );

    Integer updateContentMetaById(
            @Param("id") Long id,
            @Param("originFileName") String originFileName,
            @Param("contentType") String contentType,
            @Param("fileSize") Long fileSize,
            @Param("idempotentKey") String idempotentKey,
            @Param("updatedAt") Long updatedAt
    );

    Integer deleteById(Long id);
}
