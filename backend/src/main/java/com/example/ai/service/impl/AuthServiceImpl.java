package com.example.ai.service.impl;

import com.example.ai.entity.User;
import com.example.ai.pojo.AuthResponse;
import com.example.ai.pojo.LoginRequest;
import com.example.ai.pojo.RegisterRequest;
import com.example.ai.mapper.UserMapper;
import com.example.ai.security.Role;
import com.example.ai.service.AuthService;
import com.example.ai.service.OperationLogService;
import com.example.ai.utils.JWT;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    @Override
    public AuthResponse register(RegisterRequest request, Role requesterRole, String operator) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (userMapper.countByUsername(request.getUsername()) > 0) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userMapper.countByEmail(request.getEmail()) > 0) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        Role targetRole = request.getRole() == null ? Role.STUDENT : request.getRole();
        if (targetRole != Role.STUDENT && requesterRole != Role.ADMIN) {
            throw new IllegalArgumentException("Only ADMIN can create non-student accounts");
        }
        user.setRole(targetRole);
        user.setStatus(1);
        userMapper.insert(user);
        User saved = userMapper.selectById(user.getId());
        operationLogService.log(operator, "注册用户 id=" + saved.getId());
        return new AuthResponse(saved.getId(), saved.getUsername(), saved.getRole(), saved.getEmail(), saved.getStatus(), null);
    }

    @Override
    public AuthResponse login(LoginRequest request, String operator) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new IllegalArgumentException("User is disabled");
        }
        if (!matchesPassword(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        String token = JWT.generateJWT(claims);
        operationLogService.log(operator, "登录用户 id=" + user.getId());
        return new AuthResponse(user.getId(), user.getUsername(), user.getRole(), user.getEmail(), user.getStatus(), token);
    }

    private boolean matchesPassword(String raw, String stored) {
        if (stored == null) {
            return false;
        }
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return passwordEncoder.matches(raw, stored);
        }
        return stored.equals(raw);
    }
}
