package com.example.ai.service;

import com.example.ai.entity.Student;

import java.util.List;
import java.util.Optional;

public interface StudentService {
    Student create(Student student, String operator);
    List<Student> list();
    Optional<Student> get(Integer id);
    Optional<Student> update(Integer id, Student student, String operator);
    boolean delete(Integer id, String operator);
}
