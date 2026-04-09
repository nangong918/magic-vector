package com.demo.domain.constant;

import java.util.HashMap;
import java.util.Map;

public interface ExceptionEnums {
    String getCode();

    String getMessage();

    default Map<String, String> getDataMap() {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("code", getCode());
        dataMap.put("message", getMessage());
        return dataMap;
    }
}
