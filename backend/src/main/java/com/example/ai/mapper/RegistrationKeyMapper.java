package com.example.ai.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface RegistrationKeyMapper {

    @Select("""
            SELECT id, key_value AS keyValue, used, used_by AS usedBy,
                   created_at AS createdAt, used_at AS usedAt
            FROM registration_keys
            WHERE key_value = #{keyValue}
            """)
    Map<String, Object> selectByKeyValue(String keyValue);

    @Update("""
            UPDATE registration_keys
            SET used = 1, used_by = #{userId}, used_at = NOW()
            WHERE key_value = #{keyValue} AND used = 0
            """)
    int markUsed(@Param("keyValue") String keyValue, @Param("userId") Integer userId);

    @Insert("INSERT INTO registration_keys (key_value) VALUES (#{keyValue})")
    int insert(String keyValue);
}
