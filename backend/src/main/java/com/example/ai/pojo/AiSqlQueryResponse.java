package com.example.ai.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSqlQueryResponse {

    /**
     * 经过后端校验和重写后的最终 SQL。
     */
    private String sql;

    /**
     * 实际返回的记录数。
     */
    private int rowCount;

    /**
     * MyBatis 查询结果。
     */
    private List<Map<String, Object>> rows;
}
