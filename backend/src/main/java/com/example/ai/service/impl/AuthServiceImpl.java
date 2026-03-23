package com.example.ai.service.impl;

import com.example.ai.entity.User;
import com.example.ai.mapper.RegistrationKeyMapper;
import com.example.ai.mapper.UserMapper;
import com.example.ai.pojo.AuthResponse;
import com.example.ai.pojo.LoginRequest;
import com.example.ai.pojo.RegisterRequest;
import com.example.ai.security.Role;
import com.example.ai.service.AuthService;
import com.example.ai.service.OperationLogService;
import com.example.ai.utils.JWT;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles registration and login, including role checks and token creation.
 */
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

        // Encode the password before persisting the user record.
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        // Default to the student role unless the caller explicitly selects one.
        Role targetRole = request.getRole() == null ? Role.STUDENT : request.getRole();
        if (targetRole == Role.TEACHER) {
            String key = request.getRegistrationKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Teacher registration requires a registration key");
            }
            Map<String, Object> keyRecord = registrationKeyMapper.selectByKeyValue(key);
            if (keyRecord == null) {
                throw new IllegalArgumentException("Registration key is invalid");
            }
            if ((int) keyRecord.get("used") == 1) {
                throw new IllegalArgumentException("Registration key has already been used");
            }
        } else if (targetRole == Role.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be self-registered");
        }

        user.setRole(targetRole);
        user.setStatus(1);
        userMapper.insert(user);
        User saved = userMapper.selectById(user.getId());

        // Mark the teacher registration key as consumed after the user is saved.
        if (targetRole == Role.TEACHER) {
            registrationKeyMapper.markUsed(request.getRegistrationKey(), saved.getId());
        }
        operationLogService.log(operator, "Registered user id=" + saved.getId());
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
            throw new IllegalArgumentException("Invalid username or password");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new IllegalArgumentException("Account is disabled");
        }

        // Put the minimum required fields into the JWT claims.
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("username", user.getUsername());
        claims.put("role", user.getRole().name());
        String token = JWT.generateJWT(claims);

        operationLogService.log(operator, "Logged in user id=" + user.getId());
        return new AuthResponse(user.getId(), user.getUsername(), user.getRole(), user.getEmail(), user.getStatus(), token);
    }

    private boolean matchesPassword(String raw, String stored) {
        if (stored == null) {
            return false;
        }
        return passwordEncoder.matches(raw, stored);
    }
}
