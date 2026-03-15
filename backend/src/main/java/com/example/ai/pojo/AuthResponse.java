package com.example.ai.pojo;

import com.example.ai.security.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Integer id;
    private String username;
    private Role role;
    private String email;
    private Integer status;
    private String token;
}
