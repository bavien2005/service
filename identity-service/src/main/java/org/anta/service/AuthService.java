package org.anta.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.dto.request.AdminCreateUserRequest;
import org.anta.dto.request.RegisterRequest;
import org.anta.entity.AuditLog;
import org.anta.entity.PasswordResetToken;
import org.anta.entity.User;
import org.anta.enums.Role;
import org.anta.mapper.UserMapper;
import org.anta.repository.AuditLogRepository;
import org.anta.repository.PasswordResetTokenRepository;
import org.anta.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    @Inject
    PasswordResetTokenRepository passwordResetTokenRepository;

    @Inject
    AuditLogRepository auditLogRepository;

    @Inject
    UserMapper userMapper;

    @Transactional
    public User register(RegisterRequest request) {
        try {
            if (userRepository.existsByName(request.getName())) {
                throw new RuntimeException("Name is already exits");
            }
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email is already exits");
            }

            User user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .phoneNumber(request.getPhoneNumber())
                    .role(request.getRole() != null ? request.getRole() : Role.USER)
                    .build();

            return userRepository.save(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Register failure: " + e.getMessage());
        }
    }

    @Transactional
    public User login(String nameOrEmail, String rawPassword) {
        User user;

        if (nameOrEmail.contains("@")) {
            user = userRepository.findByEmail(nameOrEmail.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with: " + nameOrEmail));
        } else {
            user = userRepository.findByName(nameOrEmail.trim())
                    .orElseThrow(() -> new RuntimeException("User not found with: " + nameOrEmail));
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Password is incorrect");
        }

        return user;
    }

    @Transactional
    public String createResetCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        String resetCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(resetCode)
                .expiryAt(LocalDateTime.now().plusMinutes(15))
                .build());

        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action("RESET_TOKEN_CREATED")
                .ipAddress("N/A")
                .userAgent("API_CALL")
                .build());

        return resetCode;
    }

    @Transactional
    public boolean verifyResetCode(String email, String code) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(code);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken token = tokenOpt.get();

        if (!token.getUser().getId().equals(user.getId())) {
            return false;
        }

        if (token.getExpiryAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action("RESET_TOKEN_VERIFIED")
                .ipAddress("N/A")
                .userAgent("API_CALL")
                .build());

        return true;
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        user.setPassword(passwordEncoder.encode(newPassword));

        passwordResetTokenRepository.markTokensAsUsedByUserId(user.getId());

        auditLogRepository.save(AuditLog.builder()
                .user(user)
                .action("PASSWORD_RESET_SUCCESS")
                .ipAddress("N/A")
                .userAgent("API_CALL")
                .build());

        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User createUserByAdmin(AdminCreateUserRequest request) {
        if (userRepository.existsByName(request.getName())) {
            throw new RuntimeException("Name is already exits");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already exits");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .build();

        return userRepository.save(user);
    }
}