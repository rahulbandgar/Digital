package com.example.userregistration.service;

import com.example.userregistration.entity.User;
import com.example.userregistration.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("A user with email '" + user.getEmail() + "' already exists.");
        }
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> updateKycStatus(Long id, String kycStatus) {
        return userRepository.findById(id).map(user -> {
            user.setKycStatus(kycStatus);
            return userRepository.save(user);
        });
    }
}
