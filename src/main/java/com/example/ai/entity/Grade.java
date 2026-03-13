package com.example.ai.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Grade {

    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private BigDecimal score;
    private Integer semester;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
