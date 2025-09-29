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

    /// 增
    // 单个增
    Long insert(OssDo ossDo);
    // 批量增
    List<Long> insertBatch(@Param("list") List<OssDo> ossDos);

    /// 删
    // 删
    void delete(Long id);
    // 根据 fileStorageName + bucketName 删除
    void deleteByObjectNameAndBucketName(
            @Param("objectName")String objectName,
            @Param("bucketName")String bucketName
    );
    // 批量删
    void deleteBatch(@Param("list") List<Long> ids);

    /// 改
    // 单个改
    void update(OssDo ossDo);
    // 批量改
    void updateBatch(@Param("list") List<OssDo> ossDos);

    /// 查
    // 根据id 查询
    OssDo getById(Long id);
    // 根据fileStorageName + bucketName查询
    OssDo getByObjectNameAndBucketName(
            @Param("objectName")String objectName,
            @Param("bucketName")String bucketName
    );
    List<OssDo> getByIds(@Param("list") List<Long> ids);
}
