package com.example.ai.service.impl;

import com.example.ai.entity.User;
import com.example.ai.pojo.AuthResponse;
import com.example.ai.pojo.LoginRequest;
import com.example.ai.pojo.RegisterRequest;
import com.example.ai.mapper.RegistrationKeyMapper;
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
    private final RegistrationKeyMapper registrationKeyMapper;

    @Override
    public AuthResponse register(RegisterRequest request, String operator) {
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
        if (targetRole == Role.TEACHER) {
            String key = request.getRegistrationKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("教师注册需要提供注册密钥");
            }
            Map<String, Object> keyRecord = registrationKeyMapper.selectByKeyValue(key);
            if (keyRecord == null) {
                throw new IllegalArgumentException("注册密钥无效");
            }
            if ((int) keyRecord.get("used") == 1) {
                throw new IllegalArgumentException("注册密钥已被使用");
            }
        } else if (targetRole == Role.ADMIN) {
            throw new IllegalArgumentException("不允许注册管理员账号");
        }
        user.setRole(targetRole);
        user.setStatus(1);
        userMapper.insert(user);
        User saved = userMapper.selectById(user.getId());
        if (targetRole == Role.TEACHER) {
            registrationKeyMapper.markUsed(request.getRegistrationKey(), saved.getId());
        }
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
        if (user == null || !matchesPassword(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被禁用");
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
        return passwordEncoder.matches(raw, stored);
    }
}
