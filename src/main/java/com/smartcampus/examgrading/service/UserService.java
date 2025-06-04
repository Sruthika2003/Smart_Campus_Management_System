package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Update only allowed fields
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setEmail(user.getEmail());

        return userRepository.save(existingUser);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        User existingUser = userRepository.findById(user.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Get the stored password from the database
        String storedPassword = existingUser.getPassword();
        
        // Verify current password using direct comparison since passwords in db.sql are stored as plain text
        if (!currentPassword.equals(storedPassword)) {
            throw new RuntimeException("Current password is incorrect");
        }

        // For new password, we'll store it as is (since we're not using encryption in the system)
        existingUser.setPassword(newPassword);
        userRepository.save(existingUser);
    }
}