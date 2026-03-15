package com.example.ai.service.impl;

import com.example.ai.entity.Course;
import com.example.ai.mapper.CourseMapper;
import com.example.ai.mapper.TeacherMapper;
import com.example.ai.service.CourseService;
import com.example.ai.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final OperationLogService operationLogService;

    @Override
    public Course create(Course course, String operator) {
        if (course.getTeacherId() != null && teacherMapper.countById(course.getTeacherId()) == 0) {
            throw new IllegalArgumentException("Teacher not found");
        }
        course.setId(null);
        courseMapper.insert(course);
        Course saved = courseMapper.selectById(course.getId());
        operationLogService.log(operator, "创建课程 id=" + saved.getId());
        return saved;
    }

    @Override
    public List<Course> list() {
        return courseMapper.selectAll();
    }

    @Override
    public Optional<Course> get(Integer id) {
        return Optional.ofNullable(courseMapper.selectById(id));
    }

    @Override
    public Optional<Course> update(Integer id, Course course, String operator) {
        return Optional.ofNullable(courseMapper.selectById(id)).map(existing -> {
            if (course.getTeacherId() != null && !course.getTeacherId().equals(existing.getTeacherId())) {
                if (teacherMapper.countById(course.getTeacherId()) == 0) {
                    throw new IllegalArgumentException("Teacher not found");
                }
                existing.setTeacherId(course.getTeacherId());
            }
            existing.setCourseName(course.getCourseName());
            existing.setCredit(course.getCredit());
            existing.setBeginDate(course.getBeginDate());
            existing.setEndDate(course.getEndDate());
            existing.setSchedule(course.getSchedule());
            existing.setDescription(course.getDescription());
            courseMapper.update(existing);
            Course saved = courseMapper.selectById(existing.getId());
            operationLogService.log(operator, "更新课程 id=" + saved.getId());
            return saved;
        });
    }

    @Override
    public boolean delete(Integer id, String operator) {
        if (courseMapper.countById(id) == 0) {
            return false;
        }
        courseMapper.deleteById(id);
        operationLogService.log(operator, "删除课程 id=" + id);
        return true;
    }
}
