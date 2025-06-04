package com.smartcampus.examgrading.view;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.AuthService;
import com.smartcampus.examgrading.view.admin.AdminView;
import com.smartcampus.examgrading.view.faculty.FacultyView;
import com.smartcampus.examgrading.view.student.StudentView;
import com.smartcampus.examgrading.view.accounts.AccountsDashboardView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

@Route("login")
@PageTitle("Login | Smart Campus System")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final AuthService authService;
    private final SecurityService securityService;
    private final TextField usernameField = new TextField("Username");
    private final PasswordField passwordField = new PasswordField("Password");
    private final Button loginButton = new Button("Login");

    public LoginView(AuthService authService, SecurityService securityService) {
        this.authService = authService;
        this.securityService = securityService;

        // Set layout properties
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Create a container for the login form
        VerticalLayout loginContainer = new VerticalLayout();
        loginContainer.setWidth("400px");
        loginContainer.setAlignItems(Alignment.CENTER);
        loginContainer.setSpacing(true);
        loginContainer.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 0 10px rgba(0, 0, 0, 0.1)")
                .set("padding", "2em");

        // Add logo and title in a horizontal layout
        Image logo = new Image("images/logo.png", "Smart Campus Logo");
        logo.setHeight("50px");
        logo.setWidth("50px");

        H1 title = new H1("Smart Campus System");
        title.getStyle()
                .set("margin", "0")
                .set("color", "var(--lumo-primary-color)")
                .set("font-size", "2em");

        HorizontalLayout headerLayout = new HorizontalLayout(logo, title);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setSpacing(true);
        loginContainer.add(headerLayout);

        // Configure form fields with styling
        usernameField.setWidthFull();
        usernameField.getStyle()
                .set("margin-top", "1em");

        passwordField.setWidthFull();
        passwordField.getStyle()
                .set("margin-bottom", "1em");

        // Style the login button
        loginButton.setWidthFull();
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.getStyle()
                .set("margin-top", "1em")
                .set("border-radius", "4px");

        // Add login button click handler
        loginButton.addClickListener(e -> {
            String username = usernameField.getValue();
            String password = passwordField.getValue();

            if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                showNotification("Please enter both username and password", NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                handleLogin(username, password);
            } catch (Exception ex) {
                showNotification("Login error: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });

        // Add form components to the layout
        loginContainer.add(usernameField, passwordField, loginButton);

        // Add login form to main layout
        add(loginContainer);

        // Set background color and gradient for the main layout
        getStyle()
                .set("background",
                        "linear-gradient(135deg, var(--lumo-primary-color-50) 0%, var(--lumo-primary-color-10) 100%)")
                .set("padding", "var(--lumo-space-l)");
    }

    private void handleLogin(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            showNotification("Please enter both username and password", NotificationVariant.LUMO_ERROR);
            return;
        } else if (securityService != null) {
            // Use SecurityService (based on user role)
            if (securityService.login(username, password)) {
                User.Role role = securityService.getCurrentUser().getRole();

                if (role.equals(User.Role.STUDENT)) {
                    getUI().ifPresent(ui -> ui.navigate(StudentView.class));
                } else if (role.equals(User.Role.FACULTY)) {
                    getUI().ifPresent(ui -> ui.navigate(FacultyView.class));
                } else if (role.equals(User.Role.ADMIN)) {
                    getUI().ifPresent(ui -> ui.navigate(AdminView.class));
                } else if (role.equals(User.Role.ACCOUNTS)) {
                    getUI().ifPresent(ui -> ui.navigate(AccountsDashboardView.class));
                }
            } else {
                showNotification("Invalid username or password", NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
        notification.addThemeVariants(variant);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Redirect to dashboard if already logged in (if a session exists, handle that
        // logic here)
    }
}
