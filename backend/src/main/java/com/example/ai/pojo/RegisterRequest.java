package com.example.ai.pojo;

import lombok.Data;
import com.example.ai.security.Role;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private Role role;
    private String registrationKey;

    // Student minimal profile
    private String name;
    private String gender;
    private Integer grade;
    private String major;
    private String className;

    // Teacher minimal profile
    private String phone;
    private String department;
}
