package com.example.ai.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class OperatorResolver {
    private OperatorResolver() {}

    public static String resolve(HttpServletRequest request, String userHeader, String roleHeader) {
        if (userHeader != null && !userHeader.isBlank()) {
            return userHeader.trim();
        }
        Object userAttr = request == null ? null : request.getAttribute("authUser");
        if (userAttr != null && !String.valueOf(userAttr).isBlank()) {
            return String.valueOf(userAttr);
        }
        if (roleHeader != null && !roleHeader.isBlank()) {
            return roleHeader.trim();
        }
        Object roleAttr = request == null ? null : request.getAttribute("authRole");
        if (roleAttr != null && !String.valueOf(roleAttr).isBlank()) {
            return String.valueOf(roleAttr);
        }
        return "unknown";
    }
}
