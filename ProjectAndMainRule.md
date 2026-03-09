**ProjectAndMainRule**
====

这是一个嵌入式机器狗项目。

## 1. 项目模块与规则入口

App 操控平台：
* Android Compose 版本：[android](app/android)
  * 模块规则：[developAndRules.md](app/android/docs/developAndRules.md)
* Flutter 版本：[flutter](app/flutter)
  * Flutter 是 Android Compose 的同步版本；未被明确要求时，不主动同步改动。

后端服务支持：
* 主要代码：[open-api](springboot/open-api)
  * 模块规则：[developAndRules.md](springboot/docs/developAndRules.md)
* MySQL SQL 代码：[db](springboot/db)

RK 及系统 Android 程序：
* 暂未开发。

## 2. 规则集自动读取与维护规则

每次开始任务时，必须按以下顺序自动读取并遵循：
1. 总规则集：[ProjectAndMainRule.md](ProjectAndMainRule.md)
2. 主设计文档：[MainDesignDocument.md](MainDesignDocument.md)
3. 当前任务涉及模块的模块规则（如 Android/SpringBoot 的 `developAndRules.md`）
4. 当前任务涉及模块的设计文档（如 `AndroidDesignDocument.md`、`SpringBootDesignDocument.md`）

当我提出“新增规则 / 修改规则 / 补全规则”时：
* 自动将规则更新到对应规则文件（总规则或模块规则）。
* 规则变更要与当前项目约定保持一致，不允许与已有规则冲突。
* 未被我明确要求时，不修改无关模块规则。

## 3. 设计文档与开发记录规则（替代 cursorDevelopLog）

从现在开始：
* 停用 `cursorDevelopLog.md` 作为开发记录主载体（历史内容仅作参考，不再继续写入）。
* `MainDesignDocument.md` 仅作为总入口索引，不承载模块实现细节。
* 开发、设计、实现记录统一写入各模块设计文档：
  * Android -> [AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
  * SpringBoot -> [SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)
* 每次完成功能后，必须把“已实现功能”写入对应模块的 DesignDocument 章节。
* 模块 DesignDocument 必须按“功能模块/架构分层”组织内容（如启动、登录、注册、数据库、Manager、接口契约、线程模型），禁止按日期或开发日志体例编写。

实现记录最少应包含：
* 本次实现内容（接口/页面/服务/数据结构变更）。
* 关键 UML / 流程图：类图、对象图、活动图、状态机图、时序图、通讯图、线程甘特图（涉及多线程时必填）。
* 若涉及数据库（Android Room / Spring MySQL），补充数据库设计说明与图（Mermaid ER 或类图）。
* 建议记录设计模式及选择原因，便于维护与复盘。
* 若改动涉及核心理论知识（`操作系统(线程, IO)`、`计算机网络`、`数据结构`、`算法`、`计算机组成原理`、`数据库`），在实现记录中标注。

## 4. 知识库记录规则

知识库文件：[学习笔记.md](学习笔记.md)

记录策略：
* 仅当我明确发出“记录到学习笔记 / 记入知识库 / 写入学习笔记”等指令时，才允许写入 `学习笔记.md`。
* 未收到明确记录指令时，即使出现重要知识，也不能自动写入 `学习笔记.md`。
* 被要求记录时，写入内容应结构化（主题、结论、适用场景/示例），便于后续检索。

## 5. 数据库主键统一规范

* 每个表的主键字段名必须为 `id`。
* 主键类型优先使用 `Long/BIGINT`（Android Room 用 `Long`，MySQL 用 `BIGINT`）。
* 原因：整型主键在 B+Tree 索引中的比较和排序成本更低、页占用更小，可减少索引层级与页分裂概率，提升范围查询和排序性能。

## 6. 数据库 ID 传输规范

* **数据库存储**：主键和外键统一使用 `Long/BIGINT` 类型存储。
* **网络传输**：ID 在 DTO、Response、Request 等网络传输对象中必须使用 `String` 类型。
* **原因**：
  * JavaScript 的 `Number` 类型最大安全整数为 `2^53 - 1`（约 9007 万亿），而雪花算法生成的 Long ID 可能超过此范围。
  * 前端（Android/Flutter/Web）接收到超过安全范围的 Long 会丢失精度，导致 ID 不准确。
  * 使用 `String` 可以保证 ID 在所有平台和语言间传输的精度一致性。
* **转换规则**：
  * 数据库实体（Entity/Do）使用 `Long` 类型。
  * 网络传输对象（DTO/Response/Request）使用 `String` 类型。
  * 在 Converter 层进行 `Long <-> String` 转换。
  * Controller 层接收前端 String 类型 ID 时，需用 `Long.parseLong()` 转换为 Long 后再调用 Service/DAO。

## 7. Domain 转换与校验统一规范

* Domain 各层之间（`dto/entity/module/ao/vo`）转换必须使用 `Converter`，禁止在 Controller/Service 大量手写字段拷贝。
* Android 侧 `Converter` 使用接口 + 实现类方式维护。
* SpringBoot 侧 `Converter` 使用 `MapStruct` 实现，放在 `springboot/open-api/src/main/java/com/openapi/converter`。
* SpringBoot 参数校验统一使用 `jakarta.validation` 注解；参数错误统一交给全局异常拦截处理。
* SpringBoot 异常类型统一定义在 `com/openapi/domain/constant/error`。
* FormData/Multipart 接口不使用 `@Valid @RequestBody` 注解校验，统一使用 `@RequestParam/@Part` + 手动参数校验。
* SpringBoot 接口契约变更（路径、参数结构、请求体形态）必须同步 Android 请求代码与 `AndroidDesignDocument.md`。
