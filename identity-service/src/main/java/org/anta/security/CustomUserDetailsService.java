package org.anta.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.anta.entity.User;
import org.anta.repository.UserRepository;

@ApplicationScoped
public class CustomUserDetailsService {

    @Inject
    UserRepository userRepository;

    public User loadUserByUsername(String username) {
        return userRepository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}