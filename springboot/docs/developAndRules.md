# SpringBoot developAndRules

## 数据库
* 对数据库表的修改需要写在[db](../db)
* 只要涉及数据库表结构变更，必须同步修改 `springboot/db/magic_vector.sql`，并在 cursor 开发日志记录改动内容与设计原因。

## 线程
* 尽量使用现代化的线程管理和锁, 比如线程池等. 一定要特别注意资源分配和线程锁等.

## 注释
* 核心方法需要注解
* Manager比较核心, 大部分是核心代码, 需要注解
* 我希望学习一些计算机理论, 如果涉及到核心的`操作系统(线程, IO)`, `计算机网络`, `数据结构`, `算法`, `计算机组成原理`, `数据库`的知识你要标注出来.

## 分层与DTO
* Domain 分层遵循：`dto(request/response)`、`entity(Do/与表一一对应)`、`module(业务实体)`。
* 几乎每个接口都要定义 request/response DTO；response 不返回裸类型（如 Boolean）。
* Service 不直接向上层暴露 `Do`；Service 负责业务编排，对外返回 `module`、`boolean`、`void` 等业务结果。
* Mapper 负责 `Do` 的查询与落库，`Do` 不越层暴露到 Controller。
* Domain 间转换统一使用 `Converter`，SpringBoot 侧必须使用 `MapStruct`（目录：`com/openapi/converter`）。
* 禁止在 Controller/Service 中手写大段字段拷贝逻辑；应下沉到 Converter。

## 接口请求体
* 除了 Multipart/FormData 外，POST 接口统一使用单一 `@RequestBody` 请求体。
* Multipart/FormData 接口（如带文件上传）不使用 `@Valid @RequestBody` 注解校验；改用 `@RequestParam/@Part` + Controller 手动参数校验（复杂规则可下沉 Service）。
* token 校验等可扩展接口需要独立 request/response DTO，便于后续字段扩展。
* 接口参数校验统一使用 `jakarta.validation` 注解（如 `@Valid/@NotBlank/@NotNull/@Positive`）。
* 参数校验异常统一由全局异常拦截处理（`com/openapi/component/handler`），Controller 不重复手写参数错误分支。
* 业务异常类型与错误码统一维护在 `com/openapi/domain/constant/error`。
* 修改 SpringBoot 接口契约（路径/参数形态/字段）后，必须同步 Android 请求代码与 `AndroidDesignDocument.md` 的接口契约描述。

## 文档
* 你写的功能和模块，统一写入 [SpringBootDesignDocument.md](SpringBootDesignDocument.md) 的对应模块章节。
* 若涉及数据库变更，设计文档必须补充数据库表设计图（Mermaid）与变更原因说明。

