package com.example.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiSqlQueryMapper {

    /**
     * 执行已经通过安全校验的 SELECT 语句。
     *
     * 注意：这里使用了 ${sql} 动态 SQL，
     * 但调用前必须已经经过 AST 级校验与权限重写，
     * 否则不能直接暴露给用户输入。
     */
    @Select("${sql}")
    List<Map<String, Object>> executeSelect(@Param("sql") String sql);
}
