package com.example.product_api.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.product_api.model.User;
import com.example.product_api.repository.UserRepository;
import com.example.product_api.util.JwtUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Valida credenciais do usuário
     */
    public boolean validateCredentials(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            return false;
        }
        
        User user = userOptional.get();
        return passwordEncoder.matches(password, user.getPassword());
    }

    /**
     * Gera access token e refresh token
     */
    public String[] generateTokens(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        String permission = userOptional.map(User::getPermission).orElse("USER");
        String accessToken = jwtUtil.generateAccessToken(username, permission);
        String refreshToken = jwtUtil.generateRefreshToken(username);
        
        // Salva refresh token no banco
        saveRefreshToken(username, refreshToken);
        
        return new String[]{accessToken, refreshToken};
    }

    /**
     * Salva refresh token no banco
     */
    public void saveRefreshToken(String username, String refreshToken) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setRefreshToken(refreshToken);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        }
    }

    /**
     * Valida refresh token e gera novo access token
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            // Valida o refresh token
            String username = jwtUtil.extractUsername(refreshToken);
            
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new RuntimeException("Token fornecido não é um refresh token");
            }
            
            // Busca usuário no banco
            Optional<User> userOptional = userRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                throw new RuntimeException("Usuário não encontrado");
            }
            
            User user = userOptional.get();
            
            // Verifica se o refresh token é o mesmo salvo no banco
            if (!refreshToken.equals(user.getRefreshToken())) {
                throw new RuntimeException("Refresh token inválido");
            }
            
            // Valida o token
            if (!jwtUtil.validateToken(refreshToken, username)) {
                throw new RuntimeException("Refresh token expirado");
            }
            
            // Gera novo access token com permission
            return jwtUtil.generateAccessToken(username, user.getPermission());
            
        } catch (Exception e) {
            throw new RuntimeException("Erro ao renovar token: " + e.getMessage());
        }
    }

    /**
     * Revoga refresh token (logout)
     */
    public void revokeRefreshToken(String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setRefreshToken(null);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
        }
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
