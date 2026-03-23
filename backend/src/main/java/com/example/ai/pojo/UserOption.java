package com.example.ai.pojo;

import com.example.ai.security.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserOption {
    private Integer id;
    private String username;
    private Role role;
}
