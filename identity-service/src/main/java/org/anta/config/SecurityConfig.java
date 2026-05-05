package org.anta.config;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ApplicationScoped
public class SecurityConfig {

    @Produces
    @ApplicationScoped
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}