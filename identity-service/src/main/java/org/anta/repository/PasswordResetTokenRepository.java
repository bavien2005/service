package org.anta.repository;


import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import org.anta.entity.PasswordResetToken;

import java.time.LocalDateTime;
import java.util.Optional;

@ApplicationScoped
public class PasswordResetTokenRepository implements PanacheRepository<PasswordResetToken> {

    public Optional<PasswordResetToken> findByToken(String token) {
        return find("token", token).firstResultOptional();
    }

    public PasswordResetToken save(PasswordResetToken token) {
        persist(token);
        return token;
    }

    public void markTokensAsUsedByUserId(Long userId) {
        update("used = true where user.id = ?1", userId);
    }

    public void deleteExpiredTokens() {
        delete("expiryAt < ?1", LocalDateTime.now());
    }
}
