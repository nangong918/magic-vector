package com.demo.cpp.navigation.interfaceConfig.entity;


import com.demo.cpp.navigation.GsonManager;
import com.demo.cpp.navigation.interfaceConfig.constant.NavigationSwitchEnum;
import com.demo.cpp.navigation.interfaceConfig.constant.NavigationTypeEnum;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 导航栏数据类
 * </p>
 * { "type": 0, "status": 2 }
 * @author Lemon
 * @since 2025-12-25 17:34:51
 */
public class NavigationEntity {
    /**
     * 导航栏类型
     * @see NavigationTypeEnum
     */
    public int type = NavigationTypeEnum.SCREEN_MANAGEMENT.getCode();
    /**
     * 状态
     * @see NavigationSwitchEnum
     */
    public int status = NavigationSwitchEnum.OPEN.getCode();

    // 获取初始化导航栏数据
    public static List<NavigationEntity> getInitNavigationList() {
        List<NavigationEntity> navigationArr = new ArrayList<>(NavigationTypeEnum.values().length);
        for (NavigationTypeEnum navigationTypeEnum : NavigationTypeEnum.values()) {
            NavigationEntity navigationEntity = new NavigationEntity();
            navigationEntity.type = navigationTypeEnum.getCode();
            navigationEntity.status = NavigationSwitchEnum.OPEN.getCode();
            navigationArr.add(navigationEntity);
        }
        // 屏幕管理 特殊设置不可关闭
        navigationArr.get(0).status = NavigationSwitchEnum.NOT_CLOSE.getCode();
        return navigationArr;
    }

    // Java to JSON
    public static String getInitNavigationListJson() {
        List<NavigationEntity> navigationArr = NavigationEntity.getInitNavigationList();
        return GsonManager.getGson().toJson(navigationArr);
    }

    /**
     * JSON to Java
     * @param json   JSON
     * @return  List<NavigationEntity>
     * @throws JsonParseException    JSON转换异常
     */
    public static List<NavigationEntity> getInitNavigationListFromJson(String json) throws JsonParseException {
        if (json == null || json.isEmpty()){
            return NavigationEntity.getInitNavigationList();
        }
        final Type NAVIGATION_LIST_TYPE = new TypeToken<List<NavigationEntity>>() {}.getType();
        return GsonManager.getGson().fromJson(json, NAVIGATION_LIST_TYPE);
    }
}
