package com.example.ai.service;

import com.example.ai.entity.Teacher;

import java.util.List;
import java.util.Optional;

public interface TeacherService {
    Teacher create(Teacher teacher, String operator);
    List<Teacher> list();
    Optional<Teacher> get(Integer id);
    Optional<Teacher> update(Integer id, Teacher teacher, String operator, Integer actorUserId, String actorRole);
    boolean delete(Integer id, String operator);
}
