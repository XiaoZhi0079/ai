package com.example.ai.pojo;

import com.example.ai.security.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserView {
    private Integer id;
    private String username;
    private Role role;
    private String email;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
