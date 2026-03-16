package com.example.ai.mapper;

import com.example.ai.entity.Student;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StudentMapper {

    @Insert("""
            INSERT INTO students (user_id, name, gender, grade, major, class_name, dormitory, guardian_phone, created_at, updated_at)
            VALUES (#{userId}, #{name}, #{gender}, #{grade}, #{major}, #{className}, #{dormitory}, #{guardianPhone}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student student);

    @Update("""
            UPDATE students
            SET user_id = #{userId},
                name = #{name},
                gender = #{gender},
                grade = #{grade},
                major = #{major},
                class_name = #{className},
                dormitory = #{dormitory},
                guardian_phone = #{guardianPhone},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(Student student);

    @Delete("DELETE FROM students WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("""
            SELECT id, user_id AS userId, name, gender, grade, major,
                   class_name AS className, dormitory, guardian_phone AS guardianPhone,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM students
            WHERE id = #{id}
            """)
    Student selectById(Integer id);

    @Select("""
            SELECT id, user_id AS userId, name, gender, grade, major,
                   class_name AS className, dormitory, guardian_phone AS guardianPhone,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM students
            ORDER BY id ASC
            """)
    List<Student> selectAll();

    @Select("SELECT COUNT(1) FROM students WHERE id = #{id}")
    int countById(Integer id);

    @Select("SELECT COUNT(1) FROM students WHERE user_id = #{userId}")
    int countByUserId(Integer userId);
}
