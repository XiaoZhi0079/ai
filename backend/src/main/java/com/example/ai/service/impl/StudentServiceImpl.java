package com.example.ai.service.impl;

import com.example.ai.entity.Student;
import com.example.ai.entity.User;
import com.example.ai.mapper.StudentMapper;
import com.example.ai.mapper.UserMapper;
import com.example.ai.security.Role;
import com.example.ai.service.OperationLogService;
import com.example.ai.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;
    private final UserMapper userMapper;
    private final OperationLogService operationLogService;

    @Override
    public Student create(Student student, String operator) {
        validateUser(student.getUserId());
        if (studentMapper.countByUserId(student.getUserId()) > 0) {
            throw new IllegalArgumentException("Student for this user already exists");
        }
        student.setId(null);
        studentMapper.insert(student);
        Student saved = studentMapper.selectById(student.getId());
        operationLogService.log(operator, "创建学生 id=" + saved.getId());
        return saved;
    }

    @Override
    public List<Student> list() {
        return studentMapper.selectAll();
    }

    @Override
    public Optional<Student> get(Integer id) {
        return Optional.ofNullable(studentMapper.selectById(id));
    }

    @Override
    public Optional<Student> update(Integer id, Student student, String operator) {
        return Optional.ofNullable(studentMapper.selectById(id)).map(existing -> {
            if (student.getUserId() != null && !student.getUserId().equals(existing.getUserId())) {
                validateUser(student.getUserId());
                if (studentMapper.countByUserId(student.getUserId()) > 0) {
                    throw new IllegalArgumentException("Student for this user already exists");
                }
                existing.setUserId(student.getUserId());
            }
            existing.setName(student.getName());
            existing.setGender(student.getGender());
            existing.setGrade(student.getGrade());
            existing.setMajor(student.getMajor());
            existing.setClassName(student.getClassName());
            existing.setDormitory(student.getDormitory());
            existing.setGuardianPhone(student.getGuardianPhone());
            studentMapper.update(existing);
            Student saved = studentMapper.selectById(existing.getId());
            operationLogService.log(operator, "更新学生 id=" + saved.getId());
            return saved;
        });
    }

    @Override
    public boolean delete(Integer id, String operator) {
        if (studentMapper.countById(id) == 0) {
            return false;
        }
        studentMapper.deleteById(id);
        operationLogService.log(operator, "删除学生 id=" + id);
        return true;
    }

    private void validateUser(Integer userId) {
        if (userId == null || userMapper.countById(userId) == 0) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userMapper.selectById(userId);
        if (user != null && user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("User role is not STUDENT");
        }
    }
}
