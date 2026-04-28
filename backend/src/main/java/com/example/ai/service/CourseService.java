package com.example.ai.service;

import com.example.ai.entity.Course;

import java.util.List;
import java.util.Optional;

public interface CourseService {
    Course create(Course course, String operator);
    List<Course> list();
    Optional<Course> get(Integer id);
    Optional<Course> update(Integer id, Course course, String operator);
    boolean delete(Integer id, String operator);
}
