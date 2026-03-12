package com.example.ai.service;

import com.example.ai.entity.Grade;

import java.util.List;
import java.util.Optional;

public interface GradeService {
    Grade create(Grade grade, String operator);
    List<Grade> list();
    Optional<Grade> get(Integer id);
    Optional<Grade> update(Integer id, Grade grade, String operator);
    boolean delete(Integer id, String operator);
}
