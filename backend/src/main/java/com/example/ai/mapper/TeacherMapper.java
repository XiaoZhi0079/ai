package com.example.ai.mapper;

import com.example.ai.entity.Teacher;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TeacherMapper {

    @Insert("""
            INSERT INTO teachers (user_id, name, gender, department, title, research_field, office_address, created_at, updated_at)
            VALUES (#{userId}, #{name}, #{gender}, #{department}, #{title}, #{researchField}, #{officeAddress}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Teacher teacher);

    @Update("""
            UPDATE teachers
            SET user_id = #{userId},
                name = #{name},
                gender = #{gender},
                department = #{department},
                title = #{title},
                research_field = #{researchField},
                office_address = #{officeAddress},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int update(Teacher teacher);

    @Delete("DELETE FROM teachers WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("""
            SELECT id, user_id AS userId, name, gender, department, title,
                   research_field AS researchField, office_address AS officeAddress,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM teachers
            WHERE id = #{id}
            """)
    Teacher selectById(Integer id);

    @Select("""
            SELECT id, user_id AS userId, name, gender, department, title,
                   research_field AS researchField, office_address AS officeAddress,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM teachers
            ORDER BY id ASC
            """)
    List<Teacher> selectAll();

    @Select("SELECT COUNT(1) FROM teachers WHERE id = #{id}")
    int countById(Integer id);

    @Select("SELECT COUNT(1) FROM teachers WHERE user_id = #{userId}")
    int countByUserId(Integer userId);
}
