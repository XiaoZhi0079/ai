package com.example.ai.service;

import com.example.ai.pojo.AiSqlQueryResponse;

public interface AiSqlQueryService {

    /**
     * 将自然语言问题转换成安全 SQL，执行后返回结果。
     */
    AiSqlQueryResponse query(String question, String modelName, Integer userId, String role);
}
