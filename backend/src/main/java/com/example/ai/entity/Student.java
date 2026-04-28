package com.example.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Student {

    private Integer id;
    private Integer userId;
    private String name;
    private String gender;
    private Integer grade;
    private String major;
    private String className;
    private String dormitory;
    private String guardianPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
