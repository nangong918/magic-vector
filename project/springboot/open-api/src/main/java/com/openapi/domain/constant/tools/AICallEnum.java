package com.openapi.domain.constant.tools;

/**
 *@author 13225
 *@date 2025/11/10 11:45
 * 设计模式：方法模板
 * 由于enum无法继承抽象类，所以只能使用接口
 */
public interface AICallEnum {

    String getCode();
    String getName();

    static <T extends Enum<T> & AICallEnum> String getAIDocs(Class<T> clazz) {
        StringBuilder sb = new StringBuilder();
        sb.append(clazz.getSimpleName()).append("{\n");
        for (T value : clazz.getEnumConstants()) {
            sb/*.append(value).append(":")*/.append(value.getName()).append(",\n");
        }
        sb.append("}");
        return sb.toString();
    }

}
