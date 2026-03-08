**SpringBootDesignDocument**
====

## 文档目标

本文件用于描述 SpringBoot 服务端的模块化架构设计，不使用时间线日志体例。

## 1. 整体架构分层

* **Controller 层**：接收请求、参数校验、响应封装。
* **Service 层**：认证流程编排、用户业务处理、对象转换。
* **鉴权子系统**：token 签发、会话映射、过期与一致性校验。
* **持久化层**：`UserMapper` 访问 MySQL，`UserDo` 作为数据库实体。
* **领域模型层**：`UserModule` 作为业务实体，隔离数据库对象外泄。

### 架构类图
```mermaid
classDiagram
    class UserController {
        +register()
        +login()
        +verifyAccessToken()
    }
    class UserService
    class UserServiceImpl
    class AuthTokenService
    class AuthTokenServiceImpl
    class UserMapper
    class UserDo
    class UserModule
    class UserAuthResponse
    class UserTokenVerifyResponse

    UserController --> UserService
    UserController --> AuthTokenService
    UserServiceImpl ..|> UserService
    AuthTokenServiceImpl ..|> AuthTokenService
    UserServiceImpl --> UserMapper
    UserServiceImpl --> UserModule
    UserMapper --> UserDo
```

## 2. 用户认证模块（登录 / 注册）

### 功能职责
* 提供登录与注册接口，返回统一 `UserAuthResponse`。
* 注册成功后可直接形成可用登录态响应。
* Service 层处理业务规则，Controller 层不感知 DB 对象。

### 认证活动图
```mermaid
flowchart TD
    A[登录/注册请求] --> B{参数合法?}
    B -- 否 --> C[返回参数错误]
    B -- 是 --> D[查询或创建用户]
    D --> E{业务校验通过?}
    E -- 否 --> F[返回业务错误]
    E -- 是 --> G[签发 accessToken]
    G --> H[返回 UserAuthResponse]
```

### 登录时序图
```mermaid
sequenceDiagram
    participant Client
    participant UserController
    participant UserService
    participant AuthTokenService
    participant UserMapper
    Client->>UserController: POST /user/login(UserLoginRequest)
    UserController->>UserService: checkPassword + getUserByAccount
    UserService->>UserMapper: query user
    UserService-->>UserController: UserModule
    UserController->>AuthTokenService: issueAccessToken(userId)
    UserController-->>Client: UserAuthResponse
```

## 3. Token 鉴权模块

### 功能职责
* token 签发时绑定 `userId`。
* token 校验必须同时校验 `userId + accessToken`。
* 对过期 token 进行清理，保障会话一致性。

### Token 校验通讯图
```mermaid
flowchart LR
    AndroidClient -->|userId + accessToken| UserController
    UserController --> AuthTokenService
    AuthTokenService --> TokenSessionMap
    TokenSessionMap --> AuthTokenService
    AuthTokenService --> UserController
    UserController -->|valid/message| AndroidClient
```

### Token 校验活动图
```mermaid
flowchart TD
    A[接收 verify 请求] --> B{userId/token 参数合法?}
    B -- 否 --> C[返回 valid=false + 参数错误]
    B -- 是 --> D[根据 token 读取会话]
    D --> E{会话存在?}
    E -- 否 --> F[返回 valid=false + token 无效]
    E -- 是 --> G{session.userId == request.userId?}
    G -- 否 --> F
    G -- 是 --> H{token 过期?}
    H -- 是 --> I[删除会话并返回无效]
    H -- 否 --> J[返回 valid=true]
```

## 4. 用户领域与对象转换模块

### 分层原则
* `UserDo` 仅用于 Mapper/数据库层，不直接暴露到 Controller。
* Service 层输出 `UserModule`，Controller 再映射到 Response DTO。
* DTO 协议统一使用请求体对象，保障接口演进兼容性。

### 设计模式
* **分层架构（Layered Architecture）**：Controller/Service/Mapper 职责隔离。
* **DTO 门面（DTO Facade）**：通过 Request/Response DTO 稳定对外协议。

## 5. 数据库设计（MySQL）

### 设计说明
* 用户表主键统一采用 `id BIGINT`。
* 主键和业务 `userId` 全链路统一为 `Long/BIGINT`。

### ER 图
```mermaid
erDiagram
    USER {
      long id PK
      string name
      string account
      string password
      string oss_id
    }
```

### 主键规范说明
* 使用整型主键可降低 B+Tree 比较和排序成本。
* 索引体积更小，有利于范围查询与排序性能。

## 6. 接口契约模块

### 接口清单
* `POST /user/register`：`UserRegisterRequest -> UserAuthResponse`
* `POST /user/login`：`UserLoginRequest -> UserAuthResponse`
* `POST /user/token/verify`：`UserTokenVerifyRequest -> UserTokenVerifyResponse`

### 契约原则
* 非文件上传场景使用 `@RequestBody`。
* 响应使用结构化 DTO，不返回裸布尔值。
* 校验接口要求 `userId + accessToken` 联合入参。

## 7. 并发与线程模型

### 请求线程状态图
```mermaid
stateDiagram-v2
    [*] --> RequestReceived
    RequestReceived --> ServiceCompute
    ServiceCompute --> MapperIO : query/insert
    MapperIO --> ServiceCompute
    ServiceCompute --> TokenIssue : login/register success
    TokenIssue --> ResponseWrite
    ServiceCompute --> ResponseWrite : validation fail
    ResponseWrite --> [*]
```

### 鉴权甘特图
```mermaid
gantt
    title SpringBoot 鉴权请求线程甘特图
    dateFormat  X
    axisFormat %L ms
    section HTTP-NIO线程
    接收请求/参数反序列化 :a1, 0, 10
    写回响应              :a2, 60, 10
    section 业务线程
    调用AuthTokenService校验 :b1, 10, 20
    会话匹配与过期检查       :b2, 30, 20
    section 锁与状态
    ConcurrentHashMap无阻塞读 :c1, 10, 40
```

## 8. 已知边界与后续演进

* 当前 `AuthTokenService` 为内存实现，后续建议迁移 Redis 以支持重启与扩容场景。
* 注册头像上传链路受文件网关可用性影响，后续再恢复完整上传回写流程。
















