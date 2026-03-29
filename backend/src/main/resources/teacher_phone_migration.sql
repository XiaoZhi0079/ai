ALTER TABLE teachers
    ADD COLUMN phone VARCHAR(32) NULL COMMENT '教师电话号码';

-- 重新创建 AI 查询视图，确保 teacher_phone 字段可用。
-- 请在执行本文件后，再执行 ai_role_views.sql。
