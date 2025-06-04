package com.smartcampus.examgrading.security;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.UserRepository;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.server.VaadinSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecurityService {
    private final UserRepository userRepository;
    private final SessionService sessionService;

    // In a real application, this would use Spring Security
    // This is a simplified version for demo purposes
    public boolean login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent() && user.get().getPassword().equals(password)) {
            sessionService.setCurrentUser(user.get());
            return true;
        }
        return false;
    }

    public void logout() {
        sessionService.logout();
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().close();
        }
    }

    public User getCurrentUser() {
        return sessionService.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return sessionService.isLoggedIn();
    }

    public boolean hasRole(User.Role role) {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRole() == role;
    }
}