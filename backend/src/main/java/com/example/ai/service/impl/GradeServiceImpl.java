package com.example.ai.service.impl;

import com.example.ai.entity.Grade;
import com.example.ai.mapper.CourseMapper;
import com.example.ai.mapper.GradeMapper;
import com.example.ai.mapper.StudentMapper;
import com.example.ai.service.GradeService;
import com.example.ai.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeMapper gradeMapper;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final OperationLogService operationLogService;

    @Override
    public Grade create(Grade grade, String operator) {
        if (grade.getStudentId() == null || studentMapper.countById(grade.getStudentId()) == 0) {
            throw new IllegalArgumentException("Student not found");
        }
        if (grade.getCourseId() == null || courseMapper.countById(grade.getCourseId()) == 0) {
            throw new IllegalArgumentException("Course not found");
        }
        grade.setId(null);
        gradeMapper.insert(grade);
        Grade saved = gradeMapper.selectById(grade.getId());
        operationLogService.log(operator, "创建成绩 id=" + saved.getId());
        return saved;
    }

    @Override
    public List<Grade> list() {
        return gradeMapper.selectAll();
    }

    @Override
    public Optional<Grade> get(Integer id) {
        return Optional.ofNullable(gradeMapper.selectById(id));
    }

    @Override
    public Optional<Grade> update(Integer id, Grade grade, String operator) {
        return Optional.ofNullable(gradeMapper.selectById(id)).map(existing -> {
            if (grade.getStudentId() != null && !grade.getStudentId().equals(existing.getStudentId())) {
                if (studentMapper.countById(grade.getStudentId()) == 0) {
                    throw new IllegalArgumentException("Student not found");
                }
                existing.setStudentId(grade.getStudentId());
            }
            if (grade.getCourseId() != null && !grade.getCourseId().equals(existing.getCourseId())) {
                if (courseMapper.countById(grade.getCourseId()) == 0) {
                    throw new IllegalArgumentException("Course not found");
                }
                existing.setCourseId(grade.getCourseId());
            }
            existing.setScore(grade.getScore());
            existing.setSemester(grade.getSemester());
            gradeMapper.update(existing);
            Grade saved = gradeMapper.selectById(existing.getId());
            operationLogService.log(operator, "更新成绩 id=" + saved.getId());
            return saved;
        });
    }

    @Override
    public boolean delete(Integer id, String operator) {
        if (gradeMapper.countById(id) == 0) {
            return false;
        }
        gradeMapper.deleteById(id);
        operationLogService.log(operator, "删除成绩 id=" + id);
        return true;
    }
}
