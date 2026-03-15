# API 接口文档

> 基础 URL：`http://localhost:8081`  
> 在线文档：`http://localhost:8081/doc.html`（Knife4j）

---

## 1. 认证模块 (`/auth`)

### 1.1 用户注册

```
POST /auth/register
```

**请求体**：
```json
{
  "username": "student01",
  "password": "123456",
  "email": "student01@school.com",
  "role": "STUDENT"   // 可选：ADMIN / TEACHER / STUDENT（默认 STUDENT）
}
```

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "student01",
    "role": "STUDENT",
    "email": "student01@school.com",
    "status": 1,
    "token": null
  }
}
```

**规则**：
- 非 STUDENT 角色的账号只有 ADMIN 才能创建
- 用户名和邮箱必须唯一

### 1.2 用户登录

```
POST /auth/login
```

**请求体**：
```json
{
  "username": "student01",
  "password": "123456"
}
```

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "student01",
    "role": "STUDENT",
    "email": "student01@school.com",
    "status": 1,
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

---

## 2. AI 对话模块 (`/ai`)

### 2.1 文本对话

```
POST /ai/chat
```

**请求体**：
```json
{
  "userName": "张三",
  "chatId": "conversation-uuid",
  "userInput": "今天天气怎么样？",
  "chatMode": "DIRECT",          // DIRECT / KNOWLEDGE_BASE / INTERNET_SEARCH
  "model": "qwen3.5-flash",
  "imageFiles": [                // 可选：附带图片
    {
      "imageUrl": "https://...",
      "previewUrl": "预览",
      "mimeType": "image/png"
    }
  ]
}
```

**响应**：返回 AI 生成的文本字符串

### 2.2 统一发送接口

```
POST /ai/send
```

请求体同上，返回 void。

### 2.3 获取对话历史列表

```
GET /ai/history/type/{type}
```

**参数**：`type` = `chat` 或 `rag`  
**响应**：对话 ID 列表 `["uuid-1", "uuid-2", ...]`

### 2.4 获取对话消息

```
GET /ai/history/chat/{chatId}
```

**响应**：
```json
[
  { "role": "USER", "content": "你好" },
  { "role": "ASSISTANT", "content": "你好！有什么..." }
]
```

---

## 3. RAG 知识库模块 (`/rag`)

### 3.1 上传文档

```
POST /rag/upload
Content-Type: multipart/form-data
```

**参数**：`file` - 文档文件  
**响应**：`{ "code": 0, "message": "success" }`

---

## 4. 图片上传模块 (`/upload`)

### 4.1 上传图片

```
POST /upload/images
Content-Type: multipart/form-data
```

**参数**：`files` - 图片文件列表（支持多文件）  
**响应**：
```json
[
  {
    "imageUrl": "https://java-ai-1968.oss-cn-beijing.aliyuncs.com/2026/03/xxx.png",
    "previewUrl": "预览",
    "mimeType": "image/png"
  }
]
```

---

## 5. AI 图片生成模块 (`/image`)

### 5.1 文生图

```
GET /image/chat?prompt=A cute baby sea otter
```

**响应**：返回图片 URL 字符串

---

## 6. CRUD 管理模块（需 JWT 认证）

> 以下接口均需在请求头中携带：  
> `token: <JWT Token>`  
> `X-Role: ADMIN`（或对应角色）

### 6.1 用户管理 (`/api/users`) — 仅 ADMIN

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/users` | 创建用户 |
| GET | `/api/users` | 获取用户列表 |
| GET | `/api/users/{id}` | 获取单个用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |

### 6.2 学生管理 (`/api/students`) — ADMIN / TEACHER

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/students` | 创建学生（需关联已有 STUDENT 角色的 user） |
| GET | `/api/students` | 获取学生列表 |
| GET | `/api/students/{id}` | 获取单个学生 |
| PUT | `/api/students/{id}` | 更新学生 |
| DELETE | `/api/students/{id}` | 删除学生 |

### 6.3 教师管理 (`/api/teachers`) — 仅 ADMIN

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/teachers` | 创建教师（需关联已有用户） |
| GET | `/api/teachers` | 获取教师列表 |
| GET | `/api/teachers/{id}` | 获取单个教师 |
| PUT | `/api/teachers/{id}` | 更新教师 |
| DELETE | `/api/teachers/{id}` | 删除教师 |

### 6.4 课程管理 (`/api/courses`) — ADMIN / TEACHER

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/courses` | 创建课程 |
| GET | `/api/courses` | 获取课程列表 |
| GET | `/api/courses/{id}` | 获取单个课程 |
| PUT | `/api/courses/{id}` | 更新课程 |
| DELETE | `/api/courses/{id}` | 删除课程 |

### 6.5 成绩管理 (`/api/grades`) — ADMIN / TEACHER

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/grades` | 录入成绩 |
| GET | `/api/grades` | 获取成绩列表 |
| GET | `/api/grades/{id}` | 获取单条成绩 |
| PUT | `/api/grades/{id}` | 更新成绩 |
| DELETE | `/api/grades/{id}` | 删除成绩 |
