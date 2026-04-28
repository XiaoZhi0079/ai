package com.example.ai.mapper;

import com.example.ai.entity.Course;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CourseMapper {

    @Insert("""
            INSERT INTO courses (course_name, teacher_id, credit, begin_date, end_date, schedule, description, created_at, updated_at)
            VALUES (#{courseName}, #{teacherId}, #{credit}, #{beginDate}, #{endDate}, #{schedule}, #{description}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Course course);

    @Update("""
            UPDATE courses
            SET course_name = #{courseName},
                teacher_id = #{teacherId},
                credit = #{credit},
                begin_date = #{beginDate},
                end_date = #{endDate},
                schedule = #{schedule},
                description = #{description},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(Course course);

    @Delete("DELETE FROM courses WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("""
            SELECT id, course_name AS courseName, teacher_id AS teacherId, credit,
                   begin_date AS beginDate, end_date AS endDate, schedule, description,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM courses
            WHERE id = #{id}
            """)
    Course selectById(Integer id);

    @Select("""
            SELECT id, course_name AS courseName, teacher_id AS teacherId, credit,
                   begin_date AS beginDate, end_date AS endDate, schedule, description,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM courses
            ORDER BY id ASC
            """)
    List<Course> selectAll();

    @Select("SELECT COUNT(1) FROM courses WHERE id = #{id}")
    int countById(Integer id);
}
