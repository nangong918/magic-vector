# Android developAndRules

## 架构设计
* 注意应用的生命周期, 长生命周期不应该持有短生命周期避免内存泄漏, 比如viewModel/service/application持有activity的context
* 本项目属于重构项目, 之前的代码不要删除, 所有的代码最好新加, 如果跟旧的代码重名就在前面加上`Compose`
* 项目中的全局唯一多次复用的变量和管理类应该放到`MainApplication`
* 本项目应该遵顼MVI设计模式

## Domain
* 基本的数据结构要放在domain中, dto中存放request和response, entity中存放数据库实体, module中存放业务实体
* 网络请求中，除了 `FormData/Multipart` 之外，`POST` 请求必须使用单一请求体（`dto/request`）；响应统一使用 `dto/response`，不要直接返回裸类型（如 `Boolean`）。
* 对应 SpringBoot 的 Multipart/FormData 接口，Android 必须使用 Multipart 请求（`@Multipart + @Part`），不得私自改为 JSON 请求体。
* Android 侧请求/响应 DTO 可以直接参考 SpringBoot 同名 DTO 复制并做 Android 语法微调，保证前后端结构一致、便于维护。
* MVI设计模式中, uiState, dataState, intent, effect等应该直接在viewModel中定义, 不要在domain中定义, 但是这些内部可以使用domain中的数据结构来聚合.
* Domain 间转换必须使用 `Converter`，Android 使用“接口 + 实现类”方式，不在 ViewModel/Activity 中手写大段字段拷贝。

## Dao
* 操作数据库的接口应该放在Dao中
* 复杂的业务逻辑应该设计到Room数据库中, 简单的业务逻辑直接放在MMKV就可以.
* Room 只能使用一个全局数据库：`VectorDatabase`，业务表（如 User）作为其中的表，不允许新建独立数据库实例。

## Manager
* 存放业务逻辑, 比较复杂的需要复用的业务逻辑就用Manager. 普通的业务逻辑就直接放在ViewModel中就行. 命名叫Controller也没关系.
* 可以复用的逻辑需要用Controller, 像SpringBoot中的Service
* 内部有复杂的状态值管理, 线程携程管理, 使用Manager

## Activity
* Activity应该放在activity目录下
* Activity页面不做复杂的UI设计，UI要拆分到 `com/magicvector/ui/view/activity`，Activity仅保留编排逻辑（导航、effect监听、权限触发等）。
* activity的主UI要以Activity前缀+Screen命名,并且最好要有@Preview预览函数, 其他的fragment和自定义view必须要有预览函数.

## Fragment
* 由于现在使用的是Compose, 不存在Fragment了, 但是Fragment中要方页面级别的组合函数.
  (自定义组合函数fragment和自定义view的区别是fragment可以充当整个页面, 需要viewModel, 内部view复杂或者多个自定义view组合. 而自定义view就相对简单, 没有viewModel)

## Service
* Service应该放在service目录下
* 长生命周期的操作比如ws长连接, 下载上传, 音视频编解码, 应该放在Service(持续任务)中. 当然你可以用Compose中更现代化的Worker(一次性任务)

## UI
* 自定义view放在ui.view下
* color, theme, type不适用androidX的xml而是使用compose的Color.kt, Theme.kt, Type.kt

## ViewModel
* 本项目采用MVI设计模式, viewModel中需要注意设计以下这些, 如果没有则不需要:
  uiState: ui的状态值, 用于控制页面的ui
  dataState: 数据状态, 用于存储不参与ui显示的变量例如userId, access_token
  intent: 用户意图
  effect: 界面的副作用
  event: 事件监听, 如eventBus, 广播
* `uiState/dataState/intent/effect/event` 的字段定义都要写注释，便于后续维护与重构。
* Fragment和Activity都要有vm, 自定义view不需要vm
* viewModel放在viewModel下, 要区分fragment和activity的vm

## 工具类
* 工具类放在utils下

## 常量、配置、枚举
* 常量和配置写在BaseConstant中，枚举

## 线程
* 尽量使用现代化的线程管理和锁, 比如线程池, 携程, Handler, ThreadHandler等. 一定要特别注意资源分配和线程锁等.

## 注释
* ViewModel的MVI核心设计需要注释
* 核心方法需要注解
* Manager比较核心, 大部分是核心代码, 需要注解
* 我希望学习一些计算机理论, 如果涉及到核心的`操作系统(线程, IO)`, `计算机网络`, `数据结构`, `算法`, `计算机组成原理`, `数据库`的知识你要标注出来.

## 文档
* 你写的功能和模块，统一写入 [AndroidDesignDocument.md](AndroidDesignDocument.md) 的对应模块章节。
* 若涉及数据库（Room/MySQL）调整，设计文档必须记录：表设计、字段变更、变更原因，并附数据库设计图（Mermaid ER/类图）。
* 设计文档必须“目录化、结构化”，禁止将“本次新增内容”独立追加在文档末尾形成日志块。
* 新功能内容必须插入到已有对应章节：
  * 架构变化 -> `整体架构分层` 与 `架构类图`
  * 页面变化 -> `页面模块设计`（补 UI 设计、交互、MVI 类图/通信图/活动图/时序图/线程甘特图）
  * Manager/Controller 变化 -> `Manager管理类设计`（静态 UML + 动态 UML，复杂对象关系补对象图）
  * Room/Dao/Entity 变化 -> `本地数据库设计（Room）`（优先合并 ER 图，不拆散）
  * 接口变化 -> `网络接口契约`（每个接口写“用途/功能”）
  * Domain/DTO/Converter 变化 -> 插入现有 Domain/转换相关章节
* 文档中的“通信图”归类为动态 UML，不应放在静态 UML 子章节中。

