package com.vectordemo.domain.exception

enum class UserExceptions(
    override val code: String,
    override val messageText: String
) : ExceptionEnums {
    USER_NOT_EXIST("U_10001", "用户不存在"),
    ACCOUNT_ALREADY_EXIST("U_10002", "账号已存在"),
    ACCOUNT_OR_PASSWORD_ERROR("U_10003", "账号或密码错误"),
    ACCESS_TOKEN_INVALID("U_10004", "access_token无效"),
    NO_TOKEN_FORBIDDEN_CALL_API("U_10005", "无Token禁止调用API");
}
