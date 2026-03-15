package com.example.ai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Course {

    private Integer id;
    private String courseName;
    private Integer teacherId;
    private BigDecimal credit;
    private LocalDate beginDate;
    private LocalDate endDate;
    private String schedule;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
