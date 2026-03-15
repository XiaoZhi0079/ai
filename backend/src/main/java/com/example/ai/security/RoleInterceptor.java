package com.example.ai.security;

import com.example.ai.pojo.LeeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class RoleInterceptor implements HandlerInterceptor {

    private static final String ROLE_HEADER = "X-Role";
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RoleRequired required = handlerMethod.getMethodAnnotation(RoleRequired.class);
        if (required == null) {
            required = handlerMethod.getBeanType().getAnnotation(RoleRequired.class);
        }
        if (required == null) {
            return true;
        }

        String roleHeader = request.getHeader(ROLE_HEADER);
        String roleAttr = request.getAttribute("authRole") == null ? null : String.valueOf(request.getAttribute("authRole"));
        String roleValue = (roleHeader == null || roleHeader.isBlank()) ? roleAttr : roleHeader;
        if (roleValue == null || roleValue.isBlank()) {
            writeForbidden(response, "Missing role header");
            return false;
        }

        Role userRole;
        try {
            userRole = Role.valueOf(roleValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            writeForbidden(response, "Invalid role header");
            return false;
        }

        boolean allowed = Arrays.asList(required.value()).contains(userRole);
        if (!allowed) {
            writeForbidden(response, "Forbidden");
        }
        return allowed;
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(LeeResult.fail(403, message)));
    }
}
