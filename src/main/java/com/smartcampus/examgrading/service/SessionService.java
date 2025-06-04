package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.UserRepository;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;

@Service
public class SessionService {

    private final UserRepository userRepository;
    private static final String USER_SESSION_KEY = "current_user";

    public SessionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setCurrentUser(User user) {
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setAttribute(USER_SESSION_KEY, user);
        }
    }

    public User getCurrentUser() {
        if (VaadinSession.getCurrent() != null) {
            return (User) VaadinSession.getCurrent().getAttribute(USER_SESSION_KEY);
        }
        return null;
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public boolean isFaculty() {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRole() == User.Role.FACULTY;
    }

    public boolean isAdmin() {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRole() == User.Role.ADMIN;
    }

    public boolean isStudent() {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRole() == User.Role.STUDENT;
    }

    public boolean isAccounts() {
        return isLoggedIn() && getCurrentUser().getRole() == User.Role.ACCOUNTS;
    }

    public void logout() {
        if (VaadinSession.getCurrent() != null) {
            VaadinSession.getCurrent().setAttribute(USER_SESSION_KEY, null);
        }
    }
}