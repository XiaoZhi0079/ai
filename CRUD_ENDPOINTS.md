## 权限控制（来源：毕设文档）
文档要求基于 RBAC（管理员/教师/学生）进行权限控制，并采用 JWT 进行鉴权。  
当前实现为**轻量化 RBAC 校验**：通过请求头 `X-Role` 传入角色完成鉴权（示例用法），后续可替换为 JWT 解析后的角色注入。

角色权限（按文档描述）：
- 管理员：用户、校园信息增删改查，访问控制，系统维护，与大模型交互
- 教师：校园信息表、学生表、校园信息文档增删改查，与大模型交互
- 学生：校园信息表/文档访问权限，可上传个人文档，与大模型交互

当前 API 权限映射：
- `users`：仅 `ADMIN`
- `teachers`：仅 `ADMIN`
- `students`：`ADMIN` / `TEACHER`
- `courses`：`ADMIN` / `TEACHER`
- `grades`：`ADMIN` / `TEACHER`

## 通用说明
- Base URL: `http://localhost:8081`
- 角色头（可选）：`X-Role: ADMIN|TEACHER|STUDENT`
- JWT：登录成功返回 `token`，访问 `/api/**` 时在请求头携带 `token`
- 操作人头：`X-User: 张三`（用于记录操作日志，若不传则退化为角色名）
- 响应结构：`LeeResult<T>`，成功 `code=0`

## 操作日志
所有 `/api/**` 的新增、更新、删除都会写入 `operation_logs` 表：
- `operator`：来自 `X-User`，为空则记录 `X-Role`
- `action`：操作描述（如“创建学生 id=1”）
- `created_at`：操作时间

## 登录与注册
### 注册
- `POST /auth/register`
- Body:
```json
{
  "username": "s_liu",
  "password": "123456",
  "email": "liu.student@school.edu",
  "role": "STUDENT"
}
```
- 规则：
  - 默认创建 `STUDENT` 角色
  - 若 `role` 为 `TEACHER/ADMIN`，必须携带 `X-Role: ADMIN`
  - 密码使用 BCrypt 加密存储

### 登录
- `POST /auth/login`
- Body:
```json
{
  "username": "s_liu",
  "password": "123456"
}
```
- 返回：`id/username/role/email/status`，前端可按 `role` 渲染不同页面
- 返回同时包含 `token`，用于后续 `/api/**` 鉴权

## Users（用户表）
说明：`username` 为登录账号；真实姓名在 `teachers.name` / `students.name` 中维护。
### 创建
- `POST /api/users`
- 角色：`ADMIN`
- Body:
```json
{
  "username": "t_zhang",
  "password": "123456",
  "role": "TEACHER",
  "email": "zhang.teacher@school.edu",
  "status": 1
}
```
### 查询列表
- `GET /api/users`
- 角色：`ADMIN`
### 查询详情
- `GET /api/users/{id}`
- 角色：`ADMIN`
### 更新
- `PUT /api/users/{id}`
- 角色：`ADMIN`
### 删除
- `DELETE /api/users/{id}`
- 角色：`ADMIN`

## Teachers（教师表）
### 创建
- `POST /api/teachers`
- 角色：`ADMIN`
- 约束：`user_id` 必须存在且角色为 `TEACHER`
- Body:
```json
{
  "userId": 2,
  "name": "张伟",
  "department": "计算机学院",
  "title": "副教授",
  "researchField": "AI, NLP",
  "officeAddress": "计算机楼301"
}
```
### 查询列表
- `GET /api/teachers`
- 角色：`ADMIN`
### 查询详情
- `GET /api/teachers/{id}`
- 角色：`ADMIN`
### 更新
- `PUT /api/teachers/{id}`
- 角色：`ADMIN`
### 删除
- `DELETE /api/teachers/{id}`
- 角色：`ADMIN`

## Students（学生表）
### 创建
- `POST /api/students`
- 角色：`ADMIN` / `TEACHER`
- 约束：`user_id` 必须存在且角色为 `STUDENT`
- Body:
```json
{
  "userId": 4,
  "name": "陈晨",
  "grade": 2023,
  "major": "人工智能",
  "className": "智能2301",
  "dormitory": "一号宿舍302",
  "guardianPhone": "13800000001"
}
```
### 查询列表
- `GET /api/students`
- 角色：`ADMIN` / `TEACHER`
### 查询详情
- `GET /api/students/{id}`
- 角色：`ADMIN` / `TEACHER`
### 更新
- `PUT /api/students/{id}`
- 角色：`ADMIN` / `TEACHER`
### 删除
- `DELETE /api/students/{id}`
- 角色：`ADMIN` / `TEACHER`

## Courses（课程表）
### 创建
- `POST /api/courses`
- 角色：`ADMIN` / `TEACHER`
- 约束：`teacher_id` 存在
- Body:
```json
{
  "courseName": "人工智能导论",
  "teacherId": 1,
  "credit": 3.0,
  "beginDate": "2026-03-10",
  "endDate": "2026-07-01",
  "schedule": "周一3-4节，A101",
  "description": "人工智能基础与应用"
}
```
### 查询列表
- `GET /api/courses`
- 角色：`ADMIN` / `TEACHER`
### 查询详情
- `GET /api/courses/{id}`
- 角色：`ADMIN` / `TEACHER`
### 更新
- `PUT /api/courses/{id}`
- 角色：`ADMIN` / `TEACHER`
### 删除
- `DELETE /api/courses/{id}`
- 角色：`ADMIN` / `TEACHER`

## Grades（成绩表）
### 创建
- `POST /api/grades`
- 角色：`ADMIN` / `TEACHER`
- 约束：`student_id`、`course_id` 存在
- Body:
```json
{
  "studentId": 1,
  "courseId": 1,
  "score": 88.5,
  "semester": 2026
}
```
### 查询列表
- `GET /api/grades`
- 角色：`ADMIN` / `TEACHER`
### 查询详情
- `GET /api/grades/{id}`
- 角色：`ADMIN` / `TEACHER`
### 更新
- `PUT /api/grades/{id}`
- 角色：`ADMIN` / `TEACHER`
### 删除
- `DELETE /api/grades/{id}`
- 角色：`ADMIN` / `TEACHER`
