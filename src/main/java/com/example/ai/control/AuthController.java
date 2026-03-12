package com.example.ai.control;

import com.example.ai.pojo.AuthResponse;
import com.example.ai.pojo.LeeResult;
import com.example.ai.pojo.LoginRequest;
import com.example.ai.pojo.RegisterRequest;
import com.example.ai.security.Role;
import com.example.ai.service.AuthService;
import com.example.ai.utils.OperatorResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public LeeResult<AuthResponse> register(HttpServletRequest request,
                                            @RequestBody RegisterRequest requestBody,
                                            @RequestHeader(value = "X-User", required = false) String operator,
                                            @RequestHeader(value = "X-Role", required = false) String role) {
        try {
            Role requesterRole = parseRole(role);
            return LeeResult.ok(authService.register(requestBody, requesterRole, OperatorResolver.resolve(request, operator, role)));
        } catch (IllegalArgumentException ex) {
            return LeeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/login")
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

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
