package com.example.ai.pojo;

import lombok.Data;
import com.example.ai.security.Role;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private Role role;
}
