package com.example.product_api.config;

import com.example.product_api.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor para proteger endpoints financeiros.
 * Valida token JWT para TODOS os métodos HTTP (GET, POST, PUT, DELETE, etc).
 * Apenas usuários autenticados (admin) podem acessar dados financeiros.
 */
@Component
public class FinancialAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public FinancialAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();

        // Permitir preflight CORS
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Validar token JWT para QUALQUER método HTTP
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(response, "Token de acesso ausente. Autenticação obrigatória para acessar dados financeiros.");
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

            // Validar que é admin (opcional, mas recomendado)
            // Se você quiser garantir que apenas admin acesse, adicione:
            // String permission = jwtUtil.extractPermission(token);
            // if (!"ADMIN".equals(permission)) {
            //     return unauthorized(response, "Acesso negado. Apenas administradores podem acessar dados financeiros.");
            // }

            return true;
        } catch (Exception ex) {
            return unauthorized(response, "Token inválido ou expirado");
        }
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"statusCode\":401,\"message\":\"" + message + "\"}");
        return false;
    }
}
