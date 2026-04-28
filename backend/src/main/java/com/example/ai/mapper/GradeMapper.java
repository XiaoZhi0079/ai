package com.example.ai.mapper;

import com.example.ai.entity.Grade;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GradeMapper {

    @Insert("""
            INSERT INTO grades (student_id, course_id, score, semester, created_at, updated_at)
            VALUES (#{studentId}, #{courseId}, #{score}, #{semester}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Grade grade);

    @Update("""
            UPDATE grades
            SET student_id = #{studentId},
                course_id = #{courseId},
                score = #{score},
                semester = #{semester},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(Grade grade);

    @Delete("DELETE FROM grades WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("""
            SELECT id, student_id AS studentId, course_id AS courseId, score, semester,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM grades
            WHERE id = #{id}
            """)
    Grade selectById(Integer id);

    @Select("""
            SELECT id, student_id AS studentId, course_id AS courseId, score, semester,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM grades
            ORDER BY id ASC
            """)
    List<Grade> selectAll();

    @Select("SELECT COUNT(1) FROM grades WHERE id = #{id}")
    int countById(Integer id);
}
