package org.anta.service;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.anta.dto.dashboard.UserMonthlyStatsResponse;
import org.anta.dto.request.UserRequest;
import org.anta.dto.response.UserResponse;
import org.anta.entity.User;
import org.anta.enums.Role;
import org.anta.mapper.UserMapper;
import org.anta.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordEncoder passwordEncoder;

    @Inject
    UserMapper userMapper;

    @Transactional
    public List<UserResponse> getAllUsers() {
        return userRepository.listAll()
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse addUser(UserRequest req) {
        if (userRepository.existsByName(req.getName())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = userMapper.toEntity(req);
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        User saved = userRepository.save(user);

        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest req) {
        User existing = userRepository.findByIdOptional(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

        if (req.getName() != null && !req.getName().equals(existing.getName())
                && userRepository.existsByName(req.getName())) {
            throw new RuntimeException("Username already exists");
        }

        if (req.getEmail() != null && !req.getEmail().equals(existing.getEmail())
                && userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        userMapper.updateEntityFromRequest(req, existing);

        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(req.getPassword()));
        }

        User saved = userRepository.save(existing);

        return userMapper.toResponse(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (userRepository.findByIdOptional(id).isEmpty()) {
            throw new RuntimeException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
    }

    public List<UserMonthlyStatsResponse> getUserMonthlyStatsFull(int year) {
        List<Object[]> rows = userRepository.countUsersByMonthFull(year);

        return rows.stream()
                .map(r -> new UserMonthlyStatsResponse(
                        ((Number) r[0]).intValue(),
                        ((Number) r[1]).intValue(),
                        ((Number) r[2]).longValue()
                ))
                .toList();
    }

    @Transactional
    public List<UserResponse> getAllStaff() {
        return userRepository.findAllByRole(Role.STAFF)
                .stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse addStaff(UserRequest req) {
        req.setRole(Role.STAFF);
        return addUser(req);
    }

    @Transactional
    public UserResponse updateStaff(Long id, UserRequest req) {
        req.setRole(Role.STAFF);
        return updateUser(id, req);
    }

    @Transactional
    public void deleteStaff(Long id) {
        deleteUser(id);
    }
}