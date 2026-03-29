package com.example.ai.pojo;

import lombok.Data;

@Data
public class AiSqlQueryRequest {

    /**
     * 用户自然语言问题，例如：我哪门课考了多少分？
     */
    private String question;

    /**
     * 可选模型名；为空时后端回退到默认可用模型。
     */
    private String model;
}
