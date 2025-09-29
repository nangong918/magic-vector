package com.minio.constant;

import lombok.Getter;
import lombok.Setter;


/**
 * @author 13225
 * @date 2025/4/18 11:16
 */
@Getter
@Setter
public class OssException extends RuntimeException {

    public OssException(String errMsg) {
        super(errMsg);
    }

    public OssException(String errMsg, Throwable e) {
        super(errMsg, e);
    }
}
