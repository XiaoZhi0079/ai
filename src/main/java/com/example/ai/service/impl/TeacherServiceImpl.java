package com.example.ai.service.impl;

import com.example.ai.entity.Teacher;
import com.example.ai.entity.User;
import com.example.ai.mapper.TeacherMapper;
import com.example.ai.mapper.UserMapper;
import com.example.ai.security.Role;
import com.example.ai.service.OperationLogService;
import com.example.ai.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherMapper teacherMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;

    @Override
    public Teacher create(Teacher teacher, String operator) {
        validateUser(teacher.getUserId());
        if (teacherMapper.countByUserId(teacher.getUserId()) > 0) {
            throw new IllegalArgumentException("Teacher for this user already exists");
        }
        teacher.setId(null);
        teacherMapper.insert(teacher);
        Teacher saved = teacherMapper.selectById(teacher.getId());
        operationLogService.log(operator, "创建教师 id=" + saved.getId());
        return saved;
    }

    @Override
    public List<Teacher> list() {
        return teacherMapper.selectAll();
    }

    @Override
    public Optional<Teacher> get(Integer id) {
        return Optional.ofNullable(teacherMapper.selectById(id));
    }

    @Override
    public Optional<Teacher> update(Integer id, Teacher teacher, String operator) {
        return Optional.ofNullable(teacherMapper.selectById(id)).map(existing -> {
            if (teacher.getUserId() != null && !teacher.getUserId().equals(existing.getUserId())) {
                validateUser(teacher.getUserId());
                if (teacherMapper.countByUserId(teacher.getUserId()) > 0) {
                    throw new IllegalArgumentException("Teacher for this user already exists");
                }
                existing.setUserId(teacher.getUserId());
            }
            existing.setName(teacher.getName());
            existing.setDepartment(teacher.getDepartment());
            existing.setTitle(teacher.getTitle());
            existing.setResearchField(teacher.getResearchField());
            existing.setOfficeAddress(teacher.getOfficeAddress());
            teacherMapper.update(existing);
            Teacher saved = teacherMapper.selectById(existing.getId());
            operationLogService.log(operator, "更新教师 id=" + saved.getId());
            return saved;
        });
    }

    @Override
    public boolean delete(Integer id, String operator) {
        if (teacherMapper.countById(id) == 0) {
            return false;
        }
        teacherMapper.deleteById(id);
        operationLogService.log(operator, "删除教师 id=" + id);
        return true;
    }

    private void validateUser(Integer userId) {
        if (userId == null || userMapper.countById(userId) == 0) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userMapper.selectById(userId);
        if (user != null && user.getRole() != Role.TEACHER) {
            throw new IllegalArgumentException("User role is not TEACHER");
        }
    }
}
