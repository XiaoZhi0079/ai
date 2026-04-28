-- 完整建表 SQL
-- 整理来源：
-- 1. backend/src/main/resources/建表.sql
-- 2. backend/src/main/resources/建表_会话记忆补充.sql
-- 3. backend/src/main/resources/teacher_phone_migration.sql
-- 4. backend/src/main/resources/rag_public_private_migration.sql
-- 5. backend/src/main/resources/rag_document_management_migration.sql
-- 6. backend/src/main/java 下当前实体类、Mapper、Repository 的实际字段使用情况
--
-- 说明：
-- 1. 本文件为可直接初始化数据库的最终表结构，不包含历史 ALTER 迁移语句。
-- 2. 为保持与现有代码一致，保留“逻辑关联”但不额外补充外键约束。
-- 3. students.gender 字段未出现在旧建表脚本中，但已在当前 StudentMapper 和 AI 视图中实际使用，因此纳入最终结构。

CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户唯一ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL COMMENT '角色',
    email VARCHAR(50) UNIQUE COMMENT '邮箱',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1=正常, 0=禁用',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用用户信息表';

CREATE TABLE IF NOT EXISTS teachers (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '教师信息ID',
    user_id INT NOT NULL UNIQUE COMMENT '关联 users.id',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender VARCHAR(10) NULL COMMENT '性别',
    phone VARCHAR(32) NULL COMMENT '教师电话号码',
    department VARCHAR(50) NULL COMMENT '所属院系',
    title VARCHAR(50) NULL COMMENT '职称',
    research_field TEXT NULL COMMENT '研究领域',
    office_address VARCHAR(50) NULL COMMENT '办公室地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师信息表';

CREATE TABLE IF NOT EXISTS students (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '学生信息ID',
    user_id INT NOT NULL UNIQUE COMMENT '关联 users.id',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender VARCHAR(10) NULL COMMENT '性别',
    grade INT NULL COMMENT '年级，例如 2023',
    major VARCHAR(20) NULL COMMENT '专业',
    class_name VARCHAR(50) NULL COMMENT '班级',
    dormitory VARCHAR(50) NULL COMMENT '宿舍号',
    guardian_phone VARCHAR(20) NULL COMMENT '监护人电话',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生信息表';

CREATE TABLE IF NOT EXISTS courses (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
    course_name VARCHAR(50) NOT NULL COMMENT '课程名称',
    teacher_id INT NULL COMMENT '授课教师ID，关联 teachers.id',
    credit DECIMAL(3,1) NULL COMMENT '学分',
    begin_date DATE NOT NULL COMMENT '开课时间',
    end_date DATE NOT NULL COMMENT '结课时间',
    schedule TEXT NULL COMMENT '课程安排',
    description TEXT NULL COMMENT '课程描述',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程信息表';

CREATE TABLE IF NOT EXISTS grades (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '成绩ID',
    student_id INT NOT NULL COMMENT '学生ID，关联 students.id',
    course_id INT NOT NULL COMMENT '课程ID，关联 courses.id',
    score DECIMAL(5,2) NOT NULL COMMENT '成绩分数',
    semester INT NOT NULL COMMENT '学年/学期标识',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩信息表';

CREATE TABLE IF NOT EXISTS operation_logs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operator VARCHAR(50) NOT NULL COMMENT '操作人',
    action VARCHAR(255) NOT NULL COMMENT '操作内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS registration_keys (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    key_value VARCHAR(64) NOT NULL UNIQUE COMMENT '教师注册密钥',
    used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已使用: 0=未使用, 1=已使用',
    used_by INT NULL COMMENT '使用者 user_id',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    used_at DATETIME NULL COMMENT '使用时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师注册密钥表';

CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id INT NULL COMMENT '用户ID，关联 users.id',
    conversation_uid VARCHAR(36) NOT NULL UNIQUE COMMENT '外部 chatId',
    title VARCHAR(100) NULL COMMENT '会话标题',
    type VARCHAR(16) NOT NULL DEFAULT 'chat' COMMENT '会话类型: chat/rag/other',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_conversations_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话信息表';

CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    conversation_id BIGINT NOT NULL COMMENT '关联 conversations.id',
    sender VARCHAR(20) NOT NULL COMMENT '发送者: USER/ASSISTANT/SYSTEM/TOOL',
    content TEXT NULL COMMENT '文本内容',
    image_url VARCHAR(500) NULL COMMENT '图片URL，多个值时以逗号分隔',
    media_meta TEXT NULL COMMENT '媒体扩展信息(JSON)',
    sequence INT NOT NULL COMMENT '消息顺序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    KEY idx_messages_conv_seq (conversation_id, sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话消息表';

CREATE TABLE IF NOT EXISTS rag_documents (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    oss_url VARCHAR(500) NOT NULL COMMENT 'OSS 访问地址',
    uploaded_by INT NULL COMMENT '上传者 user_id',
    owner_user_id INT NULL COMMENT '私有知识库归属 user_id，公有库为空',
    knowledge_scope VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT '知识库范围: PUBLIC/PRIVATE',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '切片数量',
    extracted_text LONGTEXT NULL COMMENT '入库文本内容',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库文档元信息表';

CREATE TABLE IF NOT EXISTS rag_ocr_user_settings (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id INT NOT NULL UNIQUE COMMENT '用户ID',
    base_url VARCHAR(255) NULL COMMENT '用户自定义 OCR baseUrl',
    api_key VARCHAR(255) NULL COMMENT '用户自定义 OCR apiKey',
    model VARCHAR(128) NULL COMMENT '用户自定义 OCR 模型名',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户 OCR 模型设置表';
