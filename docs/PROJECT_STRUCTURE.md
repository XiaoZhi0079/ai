## 项目介绍
这是一个基于 Spring Boot 3.5.5 与 Spring AI 1.1.2 的 Java 17 服务端项目，提供多模型对话、RAG 知识库问答、联网搜索问答、图片上传到阿里云 OSS，以及实验性的图像生成/视频生成能力。主要入口为 `src/main/java/com/example/ai/DemoApplication.java`。

## 技术栈与依赖要点
- 运行框架：Spring Boot 3.5.5，Java 17
- AI：Spring AI（OpenAI 兼容接口）、多平台模型配置
- 向量库：Redis Vector Store
- 检索与切分：Tika 文档读取 + TokenTextSplitter
- HTTP：OkHttp
- 存储：MySQL + JPA（依赖已引入）
- 文档：Knife4j / Springdoc OpenAPI
- 其他：Hutool、阿里云 OSS SDK

## 目录结构（关键部分）
```
.
├─ pom.xml
├─ src
│  ├─ main
│  │  ├─ java
│  │  │  └─ com/example/ai
│  │  │     ├─ DemoApplication.java
│  │  │     ├─ Factory
│  │  │     │  └─ ChatClientFactory.java
│  │  │     ├─ Tool
│  │  │     │  └─ DateTimeTools.java
│  │  │     ├─ aop
│  │  │     │  └─ Aspect1.java
│  │  │     ├─ config
│  │  │     │  ├─ ChatModelConfig.java
│  │  │     │  ├─ ChatModelProperties.java
│  │  │     │  ├─ Knife4jConfig.java
│  │  │     │  ├─ MVCconfiguration.java
│  │  │     │  └─ OkHttpConfig.java
│  │  │     ├─ control
│  │  │     │  ├─ ChatControl.java
│  │  │     │  ├─ ChatHistory.java
│  │  │     │  ├─ RagController.java
│  │  │     │  └─ UploadControl.java
│  │  │     ├─ delay
│  │  │     │  ├─ ChatHistoryPlus.java
│  │  │     │  ├─ ImageController.java
│  │  │     │  └─ VideoService.java
│  │  │     ├─ enums
│  │  │     │  ├─ ChatMode.java
│  │  │     │  └─ SSEMsgType.java
│  │  │     ├─ pojo
│  │  │     │  ├─ ChatEntity.java
│  │  │     │  ├─ ImagesResponse.java
│  │  │     │  ├─ LeeResult.java
│  │  │     │  ├─ MessagePojo.java
│  │  │     │  ├─ MessageVO.java
│  │  │     │  ├─ SearchResult.java
│  │  │     │  └─ SearXNGResponse.java
│  │  │     ├─ repository
│  │  │     │  ├─ ChatHistoryRepository.java
│  │  │     │  ├─ InMemoryChatHistoryRepositoryImpl.java
│  │  │     │  └─ JdbcChathistoryRepository.java
│  │  │     ├─ service
│  │  │     │  ├─ ChatService.java
│  │  │     │  ├─ DocumentService.java
│  │  │     │  ├─ SearXngService.java
│  │  │     │  ├─ UploadService.java
│  │  │     │  └─ impl
│  │  │     │     ├─ ChatServiceImpl.java
│  │  │     │     ├─ DocumentServiceImpl.java
│  │  │     │     ├─ SearXngServiceImpl.java
│  │  │     │     └─ UploadServiceImpl.java
│  │  │     └─ utils
│  │  │        ├─ AliyunOSSProperties.java
│  │  │        ├─ AliyunOssClientPutObject.java
│  │  │        ├─ ImageMimeDetector.java
│  │  │        └─ SSEServer.java
│  │  └─ resources
│  │     ├─ application.yml
│  │     ├─ 建表.sql
│  │     └─ 技术架构图.txt
│  └─ test
│     └─ java/com/example/ai/DemoApplicationTests.java
```

## 运行与配置要点
- 服务端口：`server.port=8081`
- 多模型平台配置：`ai.platforms`（dashscope / modelscope / iflow）
- Prompt 模板：`prompt.Default_System`、`prompt.RAG_PROMPT_TEMPLATE`、`prompt.INTERNET_SEARCH_PROMPT_TEMPLATE`
- 向量库：Redis（`spring.ai.vectorstore.redis.*`）
- 数据源：MySQL（`spring.datasource.*`）
- 搜索：SearXNG（`internet.websearch.searxng.*`）
- OpenAPI：Knife4j / Springdoc

注意：`application.yml` 中包含多处 API Key 与数据库密码，建议迁移到环境变量或密钥管理系统。

## 现有功能（已实现）
- 多平台、多模型对话
  - 通过 `ChatClientFactory` 动态构建 OpenAI 兼容 ChatClient
  - 支持模型参数（temperature/maxTokens）按配置加载
- 对话模式
  - 普通对话（DIRECT）
  - RAG 知识库问答（KNOWLEDGE_BASE）
  - 联网搜索问答（INTERNET_SEARCH）
- RAG 知识库
  - 文档上传：`POST /rag/upload`
  - 文档切分：TokenTextSplitter
  - 向量检索：Redis Vector Store
- 联网搜索
  - SearXNG 搜索聚合并排序
- 会话历史
  - 维护会话 ID 列表（内存）
  - 根据 `chatId` 从 ChatMemory 读取消息
  - 接口：`GET /ai/history/type/{type}`、`GET /ai/history/chat/{chatId}`
- 图片上传
  - 多文件上传到阿里云 OSS：`POST /upload/images`
  - MIME 类型探测（magic number + content-type 兜底）
- 图像生成（实验性）
  - `GET /image/chat` 通过 OpenAI 兼容接口生成图片 URL
- 基础能力
  - CORS 全开放
  - OpenAPI 文档
  - Actuator 依赖已引入

## 未有功能或未完成项（代码中显示为未实现/注释/空实现）
- 流式对话（SSE）
  - `SSEServer` 已存在，但控制层和流式 Chat 接口为注释或未接入
- 视频生成接口
  - `VideoService` 已实现，但控制层接口注释掉
- 持久化聊天历史
  - `JdbcChathistoryRepository` 被注释，`MessagePojo` 未被实际使用
  - `ChatHistoryRepository.delete/deleteAll` 为空实现
- 鉴权与权限控制
  - 未见任何 API 鉴权、用户体系或访问控制
- 速率限制/配额控制
  - 未见限流或配额逻辑
- 测试覆盖
  - `DemoApplicationTests` 为空，无业务测试

## 主要接口一览
- `POST /ai/chat`：文本对话（返回 String）
- `POST /ai/send`：统一对话入口（当前返回 void）
- `GET /ai/history/type/{type}`：按类型获取会话 ID 列表
- `GET /ai/history/chat/{chatId}`：获取会话消息
- `POST /rag/upload`：知识库文档上传
- `POST /upload/images`：图片上传到 OSS
- `GET /image/chat`：图片生成（实验性）
