package com.example.ai.control;

import com.example.ai.mapper.RegistrationKeyMapper;
import com.example.ai.pojo.AuthResponse;
import com.example.ai.pojo.LeeResult;
import com.example.ai.pojo.LoginRequest;
import com.example.ai.pojo.RegisterRequest;
import com.example.ai.security.Role;
import com.example.ai.security.RoleRequired;
import com.example.ai.service.AuthService;
import com.example.ai.utils.OperatorResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RegistrationKeyMapper registrationKeyMapper;

    @PostMapping("/auth/register")
    public LeeResult<AuthResponse> register(HttpServletRequest request,
                                            @RequestBody RegisterRequest requestBody,
                                            @RequestHeader(value = "X-User", required = false) String operator,
                                            @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return LeeResult.ok(authService.register(requestBody, OperatorResolver.resolve(request, operator, role)));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/auth/login")
    public LeeResult<AuthResponse> login(HttpServletRequest request,
                                         @RequestBody LoginRequest requestBody,
                                         @RequestHeader(value = "X-User", required = false) String operator,
                                         @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            return LeeResult.ok(authService.login(requestBody, OperatorResolver.resolve(request, operator, role)));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/api/registration-key")
    @RoleRequired(Role.ADMIN)
    public LeeResult<Map<String, String>> generateRegistrationKey() {
        String key = UUID.randomUUID().toString().replace("-", "");
        registrationKeyMapper.insert(key);
        return LeeResult.ok(Map.of("key", key));
    }
}
