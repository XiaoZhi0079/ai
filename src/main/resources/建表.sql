/*创建用户表*/
CREATE TABLE users (
                       id INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户唯一ID',
                       username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名（登录账号）',
                       password VARCHAR(100) NOT NULL COMMENT '加盐哈希后的密码',
                       role ENUM('ADMIN', 'TEACHER', 'STUDENT') NOT NULL COMMENT '角色',
                       email VARCHAR(50) UNIQUE COMMENT '邮箱（用于找回密码）',
                       status TINYINT DEFAULT 1 COMMENT '状态（1-正常，0-禁用）',
                       created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                       updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
/*                       last_login_time DATETIME COMMENT '最后登录时间'*/
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通用用户信息表';

/*教师专属信息表（关联users.id）*/
CREATE TABLE teachers (
                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT '教师信息ID',
                          user_id INT NOT NULL UNIQUE COMMENT '关联通用用户表.ID',
                          teacher_no VARCHAR(50) UNIQUE COMMENT '教师工号（如T2025001）',
                          department VARCHAR(50) COMMENT '所属院系（如计算机学院）',
                          title VARCHAR(50) COMMENT '职称（如教授/副教授/讲师）',
                          research_field TEXT COMMENT '研究领域（支持Emoji）',
                          office_address VARCHAR(50) COMMENT '办公室地址',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
    /*     KEY idx_user_id (user_id) -- 加索引提升关联查询性能（替代外键的核心）*/
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师专属信息表';

/*学生专属信息表（关联users.id）*/
CREATE TABLE students (
                          id INT PRIMARY KEY AUTO_INCREMENT COMMENT '学生信息ID',
                          user_id INT NOT NULL UNIQUE COMMENT '关联通用用户表.ID',
                          student_no VARCHAR(50) UNIQUE COMMENT '学号（如S2025001）',
                          grade YEAR COMMENT '年级（如2023级）',
                          major VARCHAR(20) COMMENT '专业（如人工智能）',
                          class_name VARCHAR(50) COMMENT '班级（如计科2301）',
                          dormitory VARCHAR(50) COMMENT '宿舍号（如1号楼302）',
                          guardian_phone VARCHAR(20) COMMENT '监护人电话',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
/*                          KEY idx_user_id (user_id) -- 加索引提升关联查询性能*/
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生专属信息表';

/*创建会话信息表（关联users.id）*/
CREATE TABLE conversations (
                               id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
                               user_id INT NOT NULL COMMENT '用户ID（关联users.id）',
                               title VARCHAR(100) COMMENT '会话标题（自动生成或用户自定义）',
                               created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会画信息表';

/*创建会话细节表（关联conversations.id）*/
/*注：会话顺序是否需要注明*/
CREATE TABLE messages (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
                          conversation_id BIGINT NOT NULL COMMENT '关联会话ID（关联conversations.id）',
                          sender ENUM('USER', 'AI') NOT NULL COMMENT '发送者',
    /*                 content_type ENUM('TEXT', 'IMAGE', 'MIXED') NOT NULL COMMENT '内容类型',*/
                          content TEXT COMMENT '文本内容（文本消息时非空）',
                          image_url VARCHAR(500) COMMENT '图片URL（有图片时非空）',
                          sequence INT NOT NULL COMMENT '消息在会话中的顺序（用于还原上下文）',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会画细节表';
/*RAG信息表*/

/*学生信息*/


# 课程信息（关联teachers.id）
CREATE TABLE courses (
                         id INT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID',
                         course_code VARCHAR(50) NOT NULL UNIQUE COMMENT '课程编码',
                         course_name VARCHAR(50) NOT NULL COMMENT '课程名称',
                         teacher_id INT COMMENT '授课教师ID（关联教师表.ID）',
                         credit DECIMAL(3,1) COMMENT '学分',
                         begin_date date NOT NULL COMMENT '开课时间',
                         end_date date NOT NULL COMMENT '结课时间',
                         schedule TEXT COMMENT '课程安排（如“周一3-4节，教201”）',
                         description TEXT COMMENT '课程描述',
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);

/*成绩信息（关联courses.id,students.id）*/
CREATE TABLE grades (
                        id INT PRIMARY KEY AUTO_INCREMENT COMMENT '成绩ID',
                        student_id INT NOT NULL COMMENT '学生ID（关联学生表.ID）',
                        course_id INT NOT NULL COMMENT '课程ID(关联课程表.ID)',
                        score DECIMAL(5,2) NOT NULL COMMENT '成绩分数',
                        semester YEAR NOT NULL COMMENT '学年',
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩信息表';