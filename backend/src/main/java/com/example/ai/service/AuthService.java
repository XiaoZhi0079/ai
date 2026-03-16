package com.example.ai.service;

import com.example.ai.pojo.AuthResponse;
import com.example.ai.pojo.LoginRequest;
import com.example.ai.pojo.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, String operator);
    AuthResponse login(LoginRequest request, String operator);
}
