package com.example.ai.service.impl;

import com.example.ai.entity.Student;
import com.example.ai.mapper.StudentMapper;
import com.example.ai.mapper.UserMapper;
import com.example.ai.service.OperationLogService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentServiceImplTest {

    @Test
    void updatePersistsGenderAlongsideOtherFields() {
        Student existing = student(1, 101, "张三", "男", 2024, "计算机", "1班", "1-101", "13800000000");
        Student savedAfterUpdate = student(1, 101, "李四", "女", 2025, "软件工程", "2班", "2-202", "13900000000");
        Student request = student(null, 101, "李四", "女", 2025, "软件工程", "2班", "2-202", "13900000000");
        AtomicReference<Student> updatedRecord = new AtomicReference<>();

        StudentMapper studentMapper = (StudentMapper) Proxy.newProxyInstance(
                StudentMapper.class.getClassLoader(),
                new Class[]{StudentMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "selectById" -> updatedRecord.get() == null ? existing : copyStudent(updatedRecord.get());
                    case "update" -> {
                        updatedRecord.set(copyStudent((Student) args[0]));
                        yield 1;
                    }
                    case "countByUserId", "countById" -> 1;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        UserMapper userMapper = (UserMapper) Proxy.newProxyInstance(
                UserMapper.class.getClassLoader(),
                new Class[]{UserMapper.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "countById" -> 1;
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );

        OperationLogService operationLogService = (OperationLogService) Proxy.newProxyInstance(
                OperationLogService.class.getClassLoader(),
                new Class[]{OperationLogService.class},
                (proxy, method, args) -> null
        );

        StudentServiceImpl studentService = new StudentServiceImpl(studentMapper, userMapper, operationLogService);

        Student actual = studentService.update(1, request, "ADMIN#1").orElseThrow();

        assertEquals("女", updatedRecord.get().getGender());
        assertEquals("女", actual.getGender());
        assertEquals(savedAfterUpdate.getName(), actual.getName());
        assertEquals(savedAfterUpdate.getGrade(), actual.getGrade());
    }

    private Student student(
            Integer id,
            Integer userId,
            String name,
            String gender,
            Integer grade,
            String major,
            String className,
            String dormitory,
            String guardianPhone
    ) {
        Student student = new Student();
        student.setId(id);
        student.setUserId(userId);
        student.setName(name);
        student.setGender(gender);
        student.setGrade(grade);
        student.setMajor(major);
        student.setClassName(className);
        student.setDormitory(dormitory);
        student.setGuardianPhone(guardianPhone);
        return student;
    }

    private Student copyStudent(Student source) {
        return student(
                source.getId(),
                source.getUserId(),
                source.getName(),
                source.getGender(),
                source.getGrade(),
                source.getMajor(),
                source.getClassName(),
                source.getDormitory(),
                source.getGuardianPhone()
        );
    }
}
