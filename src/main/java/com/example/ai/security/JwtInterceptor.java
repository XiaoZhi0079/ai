package com.example.ai.security;

import com.example.ai.pojo.LeeResult;
import com.example.ai.utils.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "token";
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            writeUnauthorized(response, "Missing token");
            return false;
        }
        try {
            Claims claims = JWT.parseJWT(token);
            Object id = claims.get("id");
            Object username = claims.get("username");
            Object role = claims.get("role");
            if (id != null) {
                request.setAttribute("authUserId", String.valueOf(id));
            }
            if (username != null) {
                request.setAttribute("authUser", String.valueOf(username));
            }
            if (role != null) {
                request.setAttribute("authRole", String.valueOf(role));
            }
            return true;
        } catch (Exception ex) {
            writeUnauthorized(response, "Invalid token");
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(LeeResult.fail(401, message)));
    }
}
