package com.example.ai.service.impl;

import com.example.ai.entity.Teacher;
import com.example.ai.entity.User;
import com.example.ai.mapper.TeacherMapper;
import com.example.ai.mapper.UserMapper;
import com.example.ai.security.Role;
import com.example.ai.service.OperationLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceImplTest {

    @Mock
    private TeacherMapper teacherMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private TeacherServiceImpl teacherService;

    @Test
    void adminCanUpdateAllTeacherFields() {
        Teacher existing = teacher(1, 101, "张老师", "计算机学院", "旧电话");
        Teacher request = teacher(null, 101, "李老师", "软件学院", "新电话");

        when(teacherMapper.selectById(1)).thenReturn(existing, teacher(1, 101, "李老师", "软件学院", "新电话"));
        doNothing().when(operationLogService).log(any(), any());

        Teacher saved = teacherService.update(1, request, "ADMIN#1", 1, Role.ADMIN.name()).orElseThrow();

        assertEquals("李老师", saved.getName());
        assertEquals("软件学院", saved.getDepartment());
        assertEquals("新电话", saved.getPhone());
        verify(teacherMapper).update(existing);
    }

    @Test
    void teacherCanOnlyUpdateOwnPhone() {
        Teacher existing = teacher(1, 200, "张老师", "计算机学院", "旧电话");
        Teacher request = teacher(null, 999, "被忽略的名字", "被忽略的院系", "新电话");

        when(teacherMapper.selectById(1)).thenReturn(existing, teacher(1, 200, "张老师", "计算机学院", "新电话"));
        doNothing().when(operationLogService).log(any(), any());

        Teacher saved = teacherService.update(1, request, "TEACHER#200", 200, Role.TEACHER.name()).orElseThrow();

        assertEquals("张老师", saved.getName());
        assertEquals("计算机学院", saved.getDepartment());
        assertEquals("新电话", saved.getPhone());
        verify(teacherMapper).update(existing);
    }

    @Test
    void teacherCannotUpdateOtherTeacher() {
        Teacher existing = teacher(1, 200, "张老师", "计算机学院", "旧电话");
        Teacher request = teacher(null, 200, "张老师", "计算机学院", "新电话");

        when(teacherMapper.selectById(1)).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> teacherService.update(1, request, "TEACHER#201", 201, Role.TEACHER.name())
        );

        assertEquals("Teachers can only update their own phone", ex.getMessage());
        verify(teacherMapper, never()).update(any());
    }

    @Test
    void updateReturnsEmptyWhenTeacherMissing() {
        when(teacherMapper.selectById(99)).thenReturn(null);

        Optional<Teacher> result = teacherService.update(99, teacher(null, 1, "", "", ""), "ADMIN#1", 1, Role.ADMIN.name());

        assertEquals(Optional.empty(), result);
        verify(teacherMapper, never()).update(any());
    }

    private Teacher teacher(Integer id, Integer userId, String name, String department, String phone) {
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setUserId(userId);
        teacher.setName(name);
        teacher.setDepartment(department);
        teacher.setTitle("讲师");
        teacher.setResearchField("AI");
        teacher.setOfficeAddress("A-101");
        teacher.setPhone(phone);
        return teacher;
    }

    @SuppressWarnings("unused")
    private User teacherUser(Integer id) {
        User user = new User();
        user.setId(id);
        user.setRole(Role.TEACHER);
        return user;
    }
}
