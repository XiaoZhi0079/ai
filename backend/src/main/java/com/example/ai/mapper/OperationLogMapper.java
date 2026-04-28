package com.example.ai.mapper;

import com.example.ai.entity.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface OperationLogMapper {

    @Insert("""
            INSERT INTO operation_logs (operator, action, created_at)
            VALUES (#{operator}, #{action}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);
}
