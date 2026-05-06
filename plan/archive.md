# 归档任务



### 归档日计划

#### 26年3月

| 时间   | 计划                                                                              | 完成情况                                                |
|------|---------------------------------------------------------------------------------|-----------------------------------------------------|
| 3/20 | 创建实现AbstractEventManager                                                        | 完成                                                  |
| 3/21 | 实现AgentEventManager                                                             | 完成                                                  |
| 3/22 | 根据AgentEventManager完成ChatMessageEventManager                                    | 完成                                                  |
| 3/23 | 将AgentEventManager用到MessageListPage和Vm中                                         | 完成                                                  |
| 3/24 | 重构整个流程，完成编译；让AI重构SpringBoot API                                                 | 完成                                                  |
| 3/25 | 初步调试，发现WS的Bug需要重新设计整个WS架构                                                       | 异常                                                  |
| 3/26 | 初步测试 + Control重构，Live推拉流媒体Demo测试                                                |                                                     |


#### 26年4月


| 时间   | 计划                                                                                                                       | 完成情况                                                                                                                                                                                                                                   |
|------|--------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 4/07 | 确认todo内容3条                                                                                                               |                                                                                                                                                                                                                                        |
| 4/08 | 生成并审核Agent流程，确认如何管理一次会话                                                                                                  | 放弃，时间紧任务中太难了                                                                                                                                                                                                                           |
| 4/09 | 改为跑通最小链路                                                                                                                 | 完成SpringBoot + Android，完成迁移                                                                                                                                                                                                            |
| 4/10 | 完成Flutter迁移 + 测试                                                                                                         | 完成flutter测试，完成游客模式，完成启动页和logo，minio延期到明天进行测试                                                                                                                                                                                           |
| 4/11 | 集成minio并实现oss存储和传输                                                                                                       | -                                                                                                                                                                                                                                      |
| 4/12 | 完成Minio起动脚本，完成测试，尽量完成Nginx反向代理及测试，完成nginx，minio，jar部署docker                                                              | -                                                                                                                                                                                                                                      |
| 4/13 | 完成注册的头像上传与主页的头像展示；完成flutter的完整AI调用切换为阿里百炼模型；完成同样功能部署到Android Compose；登录模式游客模式展示                                          | 完成minio反向代理，上传oss和获取url测试                                                                                                                                                                                                              |
| 4/14 | 完成minio接口全量测试 + 研究并完成Docker部署                                                                                            | 完成Android，Flutter测试OSS接口，Docker未完成                                                                                                                                                                                                     |
| 4/15 | Android，Flutter测试OSS接口                                                                                                   | 完成Android OSS重构                                                                                                                                                                                                                        |
| 4/16 | 完成 4/13 任务                                                                                                               | 完成Flutter OSS重构，审核通过，测试通过                                                                                                                                                                                                              |
| 4/17 | 研究并完成Docker部署                                                                                                            | 初步完成docker-demo                                                                                                                                                                                                                        |
| 4/18 | 完成 4/13 任务 （加班，完成部分docker任务）                                                                                             | 完成 nginx + minio部署docker （但是存在问题，无法访问docker内部minio）                                                                                                                                                                                    |
| 4/19 | 完成 4/13 任务                                                                                                               | 完成集成阿里百炼                                                                                                                                                                                                                               |
| 4/20 | Android Compose集成Flutter的聊天+VoiceAgent                                                                                   | 完成Android Compose的VoiceAgent（未测试）                                                                                                                                                                                                      |
| 4/21 | VoiceAgent测试，minio本地测试，minio docker测试                                                                                    | 完成docker无法访问minio问题排查                                                                                                                                                                                                                  |
| 4/22 | 修复minio url映射 + 本项目改为docker                                                                                              | 完成RTMP web部署docker                                                                                                                                                                                                                     |
| 4/23 | VoiceAgent测试                                                                                                             | 完成项目docker化                                                                                                                                                                                                                            |
| 4/24 | Live推流研究 + 迁移FlutterAAR App（方便Android和Flutter公用）                                                                         | 完成Live Push Stream初步迁移（未测试）                                                                                                                                                                                                            |
| 4/26 | 部署docker + 测试rtmp demo推流功能 + 方案文档梳理 + sdk导入android + 基本功能测试                                                              | Successfully deployed the project to Docker;Switched the Docker engine source; Successfully finished the RTMP live-streaming push, bu the server CPU usage is too high and the latency is relatively high, which needs to be addressed |
| 4/27 | Live Pull Demo                                                                                                           | live-streaming pull and playback of RTMP + X264 has been preliminarily completed                                                                                                                                                       |
| 4/28 | Live Push, Pull to Android and flutterDemo                                                                               | Completed live push-pull streaming and playback functionality for both Android native and Flutter cross-platform (pending testing)                                                                                                     |
| 4/29 | Review the previously implemented functions, thoroughly analyze and understand the code, and start learning about RK3588 |                                                                                                                                                                                                                                        |





