/* Seed data for tables other than conversations/messages. */
/* Assumes tables already exist (see 建表.sql). */

/* users */
INSERT INTO users (id, username, password, role, email, status, created_time, updated_time)
VALUES
  (1, 'admin',  '$2a$10$adminhashplaceholder', 'ADMIN',   'admin@example.com',        1, '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
  (2, 't_zhang','${bcrypt}teacher1',          'TEACHER', 'zhang.teacher@school.edu', 1, '2026-03-01 09:05:00', '2026-03-01 09:05:00'),
  (3, 't_li',   '${bcrypt}teacher2',          'TEACHER', 'li.teacher@school.edu',    1, '2026-03-01 09:06:00', '2026-03-01 09:06:00'),
  (4, 's_chen', '${bcrypt}student1',          'STUDENT', 'chen.student@school.edu',  1, '2026-03-01 09:10:00', '2026-03-01 09:10:00'),
  (5, 's_wang', '${bcrypt}student2',          'STUDENT', 'wang.student@school.edu',  1, '2026-03-01 09:11:00', '2026-03-01 09:11:00'),
  (6, 's_sun',  '${bcrypt}student3',          'STUDENT', 'sun.student@school.edu',   1, '2026-03-01 09:12:00', '2026-03-01 09:12:00');

/* teachers (user_id references users.id logically) */
INSERT INTO teachers (id, user_id, name, gender, phone, department, title, research_field, office_address, created_at, updated_at)
VALUES
  (1, 2, '张伟', '男', '13811110001', '计算机学院', '副教授', 'AI, NLP', '计算机楼301', '2026-03-01 10:00:00', '2026-03-01 10:00:00'),
  (2, 3, '李强', '男', '13811110002', '软件学院',   '讲师',   '软件测试', '软件楼205',  '2026-03-01 10:05:00', '2026-03-01 10:05:00');

/* students (user_id references users.id logically) */
INSERT INTO students (id, user_id, name, grade, major, class_name, dormitory, guardian_phone, created_at, updated_at)
VALUES
  (1, 4, '陈晨', 2023, '人工智能', '智能2301', '一号宿舍302', '13800000001', '2026-03-01 11:00:00', '2026-03-01 11:00:00'),
  (2, 5, '王浩', 2023, '计算机科学', '计科2302', '二号宿舍401', '13800000002', '2026-03-01 11:05:00', '2026-03-01 11:05:00'),
  (3, 6, '孙宁', 2024, '软件工程', '软工2401', '三号宿舍105', '13800000003', '2026-03-01 11:10:00', '2026-03-01 11:10:00');

/* courses (teacher_id references teachers.id logically) */
INSERT INTO courses (id, course_name, teacher_id, credit, begin_date, end_date, schedule, description, created_at, updated_at)
VALUES
  (1, '人工智能导论', 1, 3.0, '2026-03-10', '2026-07-01', '周一3-4节，A101', '人工智能基础与应用', '2026-03-01 12:00:00', '2026-03-01 12:00:00'),
  (2, '数据结构',     2, 3.5, '2026-03-10', '2026-07-01', '周三1-2节，B201', '数据结构与算法基础', '2026-03-01 12:05:00', '2026-03-01 12:05:00'),
  (3, '软件测试',     2, 2.5, '2026-03-10', '2026-07-01', '周五5-6节，C301', '测试方法与工具实践', '2026-03-01 12:10:00', '2026-03-01 12:10:00');

/* grades (student_id references students.id; course_id references courses.id logically) */
INSERT INTO grades (id, student_id, course_id, score, semester, created_at, updated_at)
VALUES
  (1, 1, 1, 88.50, 2026, '2026-06-20 09:00:00', '2026-06-20 09:00:00'),
  (2, 1, 2, 91.00, 2026, '2026-06-20 09:10:00', '2026-06-20 09:10:00'),
  (3, 2, 2, 85.00, 2026, '2026-06-20 09:20:00', '2026-06-20 09:20:00'),
  (4, 2, 1, 79.50, 2026, '2026-06-20 09:30:00', '2026-06-20 09:30:00'),
  (5, 3, 3, 93.00, 2026, '2026-06-20 09:40:00', '2026-06-20 09:40:00');
