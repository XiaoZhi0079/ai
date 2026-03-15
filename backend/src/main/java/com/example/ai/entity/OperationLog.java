package com.example.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {

    private Integer id;
    private String operator;
    private String action;
    private LocalDateTime createdAt;
}
