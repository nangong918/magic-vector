package com.demo.cpp.navigation;

import com.demo.cpp.navigation.interfaceConfig.dto.GetNavigationResp;
import com.demo.cpp.navigation.interfaceConfig.entity.NavigationEntity;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.junit.Test;

import java.util.List;

public class NavigationTest {

    @Test
    public void getInitNavigationListJson() {
        System.out.println(NavigationEntity.getInitNavigationListJson());
    }

    @Test
    public void jsonToObjectTest() { // 移除JSONException（已不用FastJSON）
        // 1. 生成初始化List并序列化（原生数组JSON，无引号）
        String json = NavigationEntity.getInitNavigationListJson();
        List<NavigationEntity> navigationList = NavigationEntity.getInitNavigationListFromJson(json);
        System.out.println("初始化List: " + navigationList);

        // 2. 关键修复：直接将List转为JsonArray，而非字符串
        com.google.gson.JsonObject settingsInfo = new com.google.gson.JsonObject();
        // 步骤1：将List转为JsonElement（JsonArray）
        com.google.gson.JsonElement jsonElement = GsonManager.getGson().toJsonTree(navigationList);
        // 步骤2：用add（而非addProperty）存入JsonObject，保留数组类型
        settingsInfo.add("navigationArr", jsonElement);

        // 3. 序列化JsonObject（此时navigationArr是原生数组，无引号/斜杠）
        String json3 = GsonManager.toJson(settingsInfo);
        System.out.println("修复后json3: " + json3);

        // 4. 解析为GetNavigationResp（正常，无异常）
        GetNavigationResp getNavigationResp = GsonManager.fromJson(json3, GetNavigationResp.class);
        System.out.println("解析后的对象: " + getNavigationResp);

        // 5. 重新序列化（验证格式正确）
        String json4 = GsonManager.toJson(getNavigationResp);
        System.out.println("最终json4: " + json4);
    }

}
