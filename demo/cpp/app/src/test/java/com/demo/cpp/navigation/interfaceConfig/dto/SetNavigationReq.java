package com.demo.cpp.navigation.interfaceConfig.dto;


import com.demo.cpp.navigation.interfaceConfig.entity.NavigationEntity;

import java.util.List;

/**
 * <p>
 * 设置导航栏数据请求体
 * </p>
 *
 * @author Lemon
 * @since 2025-12-26 09:46:04
 */
public class SetNavigationReq {
    public Data data;
    public static class Data {
        public List<NavigationEntity> navigationArr;
    }
}
