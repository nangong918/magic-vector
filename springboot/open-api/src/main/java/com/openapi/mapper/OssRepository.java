package com.openapi.mapper;

import com.openapi.domain.Do.OssDo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author 13225
 * @date 2025/9/29 16:13
 */
public interface OssRepository extends JpaRepository<OssDo, String> {
}
