# Cursor开发日志

## 2026-03-07 用户认证接口（登录/注册/token校验）

### 本次开发内容
- 新增 `UserController`：
  - `POST /user/register`
  - `POST /user/login`
  - `GET /user/token/verify`（请求头：`access_token`）
- 新增 `AuthTokenService` + `AuthTokenServiceImpl`：内存版 access_token 签发与校验。
- 新增认证响应模型 `UserAuthResponse`，统一返回 `userId/account/name/avatarUrl/accessToken`。
- 扩展 `UserService`：新增 `getUserById`、`getUserByAccount`。
- 修复 `UserServiceImpl#createUser` 在有头像分支下重复 `insert` 的问题。
- 注册接口中增加 TODO：minio 未稳定前，头像参数先接收不处理，避免影响主流程。

### 类图（Class Diagram）
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
    class UserAuthResponse

    UserController --> UserService
    UserController --> AuthTokenService
    UserServiceImpl ..|> UserService
    AuthTokenServiceImpl ..|> AuthTokenService
    UserServiceImpl --> UserMapper
```

### 对象图（Object Diagram）
```mermaid
classDiagram
    class LoginRequestContext {
        account: String
        password: String
        userDo: UserDo
        accessToken: String
    }
```

### 活动图（Activity Diagram）
```mermaid
flowchart TD
    A[Login/Register请求] --> B{参数合法?}
    B -- 否 --> C[返回参数错误]
    B -- 是 --> D[查询用户/创建用户]
    D --> E{业务校验通过?}
    E -- 否 --> F[返回业务错误]
    E -- 是 --> G[签发access_token]
    G --> H[返回UserAuthResponse]
```

### 状态机图（State Machine）
```mermaid
stateDiagram-v2
    [*] --> Anonymous
    Anonymous --> Authenticated : login/register success
    Authenticated --> Authenticated : token verify success
    Authenticated --> Anonymous : token expired/invalid
```

### 时序图（Sequence Diagram）
```mermaid
sequenceDiagram
    participant Android
    participant UserController
    participant UserService
    participant AuthTokenService

    Android->>UserController: POST /user/login
    UserController->>UserService: checkPassword/getUserByAccount
    UserController->>AuthTokenService: issueAccessToken
    UserController-->>Android: UserAuthResponse(accessToken)
    Android->>UserController: GET /user/token/verify
    UserController->>AuthTokenService: verifyAccessToken
    UserController-->>Android: true/invalid
```

### 通讯图（Communication Diagram）
```mermaid
flowchart LR
    Android --> UserController
    UserController --> UserService
    UserController --> AuthTokenService
    UserService --> UserMapper
```

### TODO
- minio 网关/反向代理未就绪阶段，`register` 头像参数暂不入库上传；后续可恢复 `MultipartFile avatar` 上传链路并返回真实可访问 URL。
- `AuthTokenService` 当前为内存实现，后续建议迁移 Redis，避免服务重启 token 全失效。

## 2026-03-08 规则对齐与分层修复（DTO/Service/接口体）

### 本次调整
- `POST /user/login` 改为 `@RequestBody UserLoginRequest`。
- `POST /user/token/verify` 改为 `@RequestBody UserTokenVerifyRequest`，响应改为 `UserTokenVerifyResponse`（可扩展结构）。
- `AuthTokenService#issueAccessToken` 入参从 `UserDo` 改为 `userId`，避免 Do 穿透业务层。
- `UserService` 不再向 Controller 暴露 `UserDo`，改为返回 `UserModule`（业务实体）。
- 新增 `UserModule`，由 Service 内部将 Mapper 返回的 `UserDo` 转换后再对外返回。

### 数据库改动说明（MySQL）
- 本次无数据库表结构修改，因此 `springboot/db/magic_vector.sql` 无变更。
- 记录原因：本次改动集中在接口契约和分层边界，不涉及表字段变更。

### 数据库图（ER）
```mermaid
erDiagram
    USER {
      string id PK
      string name
      string account
      string password
      string oss_id
    }
```

### 线程状态图（Thread State）
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

### 设计模式记录
- **分层模式（Layered Architecture）**：Controller/Service/Mapper 分工明确，防止持久化对象泄漏到接口层。
- **门面式返回（DTO Facade）**：通过 request/response DTO 封装协议，保证接口可扩展性。

### 理论知识标注
- **数据库**：Entity（Do）与 Module 分离，避免数据库结构耦合业务接口。
- **计算机网络**：统一请求体/响应体契约，减少协议演进成本。
- **操作系统（线程/IO）**：请求处理是计算 + IO 组合流程，Mapper 查询属于典型 IO 边界。