package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionService sessionService;

    public AuthService(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    public boolean login(String username, String password) {
        // In a real application, you would hash the password and compare with stored
        // hash
        // This is a simplified example
        Optional<User> userOpt = userRepository.findAll().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            sessionService.setCurrentUser(userOpt.get());
            return true;
        }

        return false;
    }

    public void logout() {
        sessionService.logout();
    }
}