package com.demo.cpp.domain.entity.jni;

public class IntMsg {
    // 字段必须公开，或提供getter（匹配C++中msg.value的访问）
    public int value;

    // ❶ 核心：必须有int参数的构造方法（匹配C++中(I)V的签名）
    public IntMsg(int value) {
        this.value = value;
    }

    // 可选：添加无参构造（防止后续扩展报错）
    public IntMsg() {}
}