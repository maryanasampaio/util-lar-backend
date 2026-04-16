package com.example.product_api.config;

import com.example.product_api.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthTokenInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthTokenInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        if (!isManagementMethod(method)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(response, "Token de acesso ausente");
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtUtil.extractUsername(token);
            boolean valid = username != null
                    && jwtUtil.isAccessToken(token)
                    && jwtUtil.validateToken(token, username);

            if (!valid) {
                return unauthorized(response, "Token inválido ou expirado");
            }

            return true;
        } catch (Exception ex) {
            return unauthorized(response, "Token inválido ou expirado");
        }
    }

    private boolean isManagementMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"statusCode\":401,\"message\":\"" + message + "\"}");
        return false;
    }
}
