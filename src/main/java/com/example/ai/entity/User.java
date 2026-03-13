package com.example.ai.entity;

import com.example.ai.security.Role;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Integer id;
    private String username;
    private String password;
    private Role role;
    private String email;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
