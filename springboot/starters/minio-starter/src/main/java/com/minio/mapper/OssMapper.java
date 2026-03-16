package com.minio.mapper;

import com.minio.domain.Do.OssDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 13225
 * @date 2025/4/17 18:14
 */
@Mapper
public interface OssMapper {

    Integer insert(OssDo ossDo);

    OssDo getById(Long id);

    List<OssDo> getByIds(@Param("list") List<Long> ids);

    OssDo getByIdempotentKey(@Param("idempotentKey") String idempotentKey);

    List<OssDo> queryByUserIdOrderByCreatedAt(
            @Param("userId") Long userId,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    List<OssDo> queryByUserIdOrderByFileSize(
            @Param("userId") Long userId,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    Integer deleteById(Long id);
}
