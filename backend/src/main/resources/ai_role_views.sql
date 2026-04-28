-- AI 查询角色视图（按前端列表页可见列设计）
--
-- 设计原则：
-- 1. 学生和教师的 AI 查询权限，与其前端列表页实际可见字段保持一致；
-- 2. 学生使用 ai_student_* 视图体系，教师使用 ai_teacher_* 视图体系；
-- 3. 管理员不使用专门视图，直接查询真实业务表白名单；
-- 4. 允许同一角色体系内多视图联查。

DROP VIEW IF EXISTS ai_student_teacher_list_view;
CREATE VIEW ai_student_teacher_list_view AS
SELECT
    t.id              AS id,
    t.name            AS name,
    t.gender          AS gender,
    t.phone           AS phone,
    t.department      AS department,
    t.title           AS title,
    t.research_field  AS research_field
FROM teachers t;

DROP VIEW IF EXISTS ai_student_student_list_view;
CREATE VIEW ai_student_student_list_view AS
SELECT
    s.id          AS id,
    s.name        AS name,
    s.gender      AS gender,
    s.grade       AS grade,
    s.major       AS major,
    s.class_name  AS class_name
FROM students s;

DROP VIEW IF EXISTS ai_student_course_list_view;
CREATE VIEW ai_student_course_list_view AS
SELECT
    c.id           AS id,
    c.course_name  AS course_name,
    t.name         AS teacher_name,
    c.credit       AS credit,
    c.schedule     AS schedule,
    c.begin_date   AS begin_date,
    c.end_date     AS end_date
FROM courses c
         LEFT JOIN teachers t ON c.teacher_id = t.id;

DROP VIEW IF EXISTS ai_student_grade_list_view;
CREATE VIEW ai_student_grade_list_view AS
SELECT
    g.id           AS id,
    s.name         AS student_name,
    c.course_name  AS course_name,
    g.score        AS score,
    g.semester     AS semester
FROM grades g
         LEFT JOIN students s ON g.student_id = s.id
         LEFT JOIN courses c ON g.course_id = c.id;

DROP VIEW IF EXISTS ai_teacher_teacher_list_view;
CREATE VIEW ai_teacher_teacher_list_view AS
SELECT
    t.id              AS id,
    t.name            AS name,
    t.gender          AS gender,
    t.phone           AS phone,
    t.department      AS department,
    t.title           AS title,
    t.research_field  AS research_field
FROM teachers t;

DROP VIEW IF EXISTS ai_teacher_student_list_view;
CREATE VIEW ai_teacher_student_list_view AS
SELECT
    s.id              AS id,
    s.name            AS name,
    s.gender          AS gender,
    s.grade           AS grade,
    s.major           AS major,
    s.class_name      AS class_name,
    s.dormitory       AS dormitory,
    s.guardian_phone  AS guardian_phone
FROM students s;

DROP VIEW IF EXISTS ai_teacher_course_list_view;
CREATE VIEW ai_teacher_course_list_view AS
SELECT
    c.id           AS id,
    c.course_name  AS course_name,
    t.name         AS teacher_name,
    c.credit       AS credit,
    c.schedule     AS schedule,
    c.begin_date   AS begin_date,
    c.end_date     AS end_date
FROM courses c
         LEFT JOIN teachers t ON c.teacher_id = t.id;

DROP VIEW IF EXISTS ai_teacher_grade_list_view;
CREATE VIEW ai_teacher_grade_list_view AS
SELECT
    g.id           AS id,
    s.name         AS student_name,
    c.course_name  AS course_name,
    g.score        AS score,
    g.semester     AS semester
FROM grades g
         LEFT JOIN students s ON g.student_id = s.id
         LEFT JOIN courses c ON g.course_id = c.id;
