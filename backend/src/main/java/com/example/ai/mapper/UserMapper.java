package com.example.ai.mapper;

import com.example.ai.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("""
            INSERT INTO users (username, password, role, email, status, created_time, updated_time)
            VALUES (#{username}, #{password}, #{role}, #{email}, #{status}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("""
            UPDATE users
            SET username = #{username},
                password = #{password},
                role = #{role},
                email = #{email},
                status = #{status},
                updated_time = NOW()
            WHERE id = #{id}
            """)
    int update(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("""
            SELECT id, username, password, role, email, status,
                   created_time AS createdTime, updated_time AS updatedTime
            FROM users
            WHERE id = #{id}
            """)
    User selectById(Integer id);

    @Select("""
            SELECT id, username, password, role, email, status,
                   created_time AS createdTime, updated_time AS updatedTime
            FROM users
            ORDER BY id ASC
            """)
    List<User> selectAll();

    @Select("""
            SELECT id, username, password, role, email, status,
                   created_time AS createdTime, updated_time AS updatedTime
            FROM users
            WHERE username = #{username}
            """)
    User selectByUsername(String username);

    @Select("SELECT COUNT(1) FROM users WHERE id = #{id}")
    int countById(Integer id);

    @Select("SELECT COUNT(1) FROM users WHERE username = #{username}")
    int countByUsername(String username);

    @Select("SELECT COUNT(1) FROM users WHERE email = #{email}")
    int countByEmail(String email);
}
