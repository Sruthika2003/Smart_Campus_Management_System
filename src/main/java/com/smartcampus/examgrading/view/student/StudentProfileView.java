package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.SessionService;
import com.smartcampus.examgrading.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route("student/profile")
@PageTitle("My Profile | Student")
public class StudentProfileView extends VerticalLayout implements BeforeEnterObserver {
    private final SessionService sessionService;
    private final UserService userService;
    private User currentUser;

    private final TextField usernameField = new TextField("Username");
    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final EmailField emailField = new EmailField("Email");
    private final PasswordField currentPasswordField = new PasswordField("Current Password");
    private final PasswordField newPasswordField = new PasswordField("New Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm New Password");
    
    private final Binder<User> binder = new Binder<>(User.class);

    public StudentProfileView(SessionService sessionService, UserService userService) {
        this.sessionService = sessionService;
        this.userService = userService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        if (!sessionService.isLoggedIn()) {
            return; // BeforeEnter will handle the redirect
        }

        this.currentUser = sessionService.getCurrentUser();

        if (!sessionService.isStudent()) {
            add(new H2("Access Denied"),
                    new Paragraph("Only students can access this page."));
            return;
        }

        setupLayout();
        setupBinder();
        loadUserData();
    }

    private void setupLayout() {
        add(new H2("My Profile"));

        // Basic Information Form
        FormLayout basicInfoForm = new FormLayout();
        usernameField.setReadOnly(true);
        basicInfoForm.add(
            usernameField,
            firstNameField,
            lastNameField,
            emailField
        );
        basicInfoForm.setColspan(usernameField, 2);
        basicInfoForm.setColspan(emailField, 2);

        // Save Basic Info Button
        Button saveBasicInfoButton = new Button("Save Changes", e -> saveBasicInfo());
        saveBasicInfoButton.addClassName("save-button");

        // Password Change Form
        FormLayout passwordForm = new FormLayout();
        passwordForm.add(
            currentPasswordField,
            newPasswordField,
            confirmPasswordField
        );

        // Change Password Button
        Button changePasswordButton = new Button("Change Password", e -> changePassword());
        changePasswordButton.addClassName("change-password-button");

        // Add components to layout
        add(
            basicInfoForm,
            saveBasicInfoButton,
            new H2("Change Password"),
            passwordForm,
            changePasswordButton
        );
    }

    private void setupBinder() {
        binder.forField(firstNameField)
            .asRequired("First name is required")
            .bind(User::getFirstName, User::setFirstName);

        binder.forField(lastNameField)
            .asRequired("Last name is required")
            .bind(User::getLastName, User::setLastName);

        binder.forField(emailField)
            .asRequired("Email is required")
            .withValidator(email -> email.matches("^[A-Za-z0-9+_.-]+@(.+)$"), "Invalid email format")
            .bind(User::getEmail, User::setEmail);
    }

    private void loadUserData() {
        usernameField.setValue(currentUser.getUsername());
        binder.readBean(currentUser);
    }

    private void saveBasicInfo() {
        try {
            if (binder.validate().isOk()) {
                binder.writeBean(currentUser);
                userService.updateUser(currentUser);
                Notification.show("Profile updated successfully!", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }
        } catch (Exception e) {
            Notification.show("Error updating profile: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void changePassword() {
        if (currentPasswordField.isEmpty() || newPasswordField.isEmpty() || confirmPasswordField.isEmpty()) {
            Notification.show("Please fill in all password fields", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (!newPasswordField.getValue().equals(confirmPasswordField.getValue())) {
            Notification.show("New passwords do not match", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            userService.changePassword(
                currentUser,
                currentPasswordField.getValue(),
                newPasswordField.getValue()
            );
            
            // Clear password fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();

            Notification.show("Password changed successfully!", 3000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Error changing password: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.isLoggedIn()) {
            event.rerouteTo("login");
        }
    }
} 