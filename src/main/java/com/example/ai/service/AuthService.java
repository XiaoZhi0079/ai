package com.example.ai.service;

import com.example.ai.pojo.AuthResponse;
import com.example.ai.pojo.LoginRequest;
import com.example.ai.pojo.RegisterRequest;
import com.example.ai.security.Role;

public interface AuthService {
    AuthResponse register(RegisterRequest request, Role requesterRole, String operator);
    AuthResponse login(LoginRequest request, String operator);
}
