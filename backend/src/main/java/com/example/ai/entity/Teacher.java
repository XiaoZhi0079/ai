package com.example.ai.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Teacher {

    private Integer id;
    private Integer userId;
    private String name;
    private String gender;
    private String department;
    private String title;
    private String researchField;
    private String officeAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
