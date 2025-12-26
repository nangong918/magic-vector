package com.demo.cpp.navigation;




import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * <p>
 * Gson解析工具类（单例模式）
 * 提供JSON字符串与Java对象的互转方法，解析异常直接抛出，由调用方处理
 * </p>
 *
 * @author Lemon
 * @since 2025-12-26 09:55:26
 */
public class GsonManager {
    public static final boolean DBG = true;
    public static final String TAG = "GsonManager";
    private GsonManager() {}

    private static final Gson gson = new Gson();
    public static Gson getGson() {
        return gson;
    }

    /**
     * JSON字符串解析为指定类型的对象（抛出异常，由调用方处理）
     * @param jsonStr 待解析的JSON字符串
     * @param clazz   目标对象的Class类型
     * @param <T>     泛型，匹配目标类型
     * @return 解析后的对象
     * @throws JsonSyntaxException JSON格式错误/类型不匹配时抛出
     */
    public static  <T> T fromJson(@NonNull String jsonStr, @NonNull Class<T> clazz) throws JsonSyntaxException {
        return gson.fromJson(jsonStr, clazz);
    }

    /**
     * 将任意对象转换为JSON字符串
     * @param obj 待转换的对象（null会转为"null"字符串）
     * @return JSON字符串
     * @throws JsonSyntaxException 对象序列化失败时抛出（如循环引用）
     */
    public static String toJson(Object obj) throws JsonSyntaxException {
        return gson.toJson(obj);
    }

}
