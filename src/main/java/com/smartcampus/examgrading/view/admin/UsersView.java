package com.smartcampus.examgrading.view.admin;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.User.Role;
import com.smartcampus.examgrading.service.UserService;
import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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

import java.util.List;

@Route(value = "admin/users", layout = MainLayout.class)
@PageTitle("User Management | Admin")
public class UsersView extends VerticalLayout {
    private final Grid<User> grid = new Grid<>(User.class);
    private final UserService userService;

    private final Button addUserButton = new Button("Add User");
    private ComboBox<Role> roleFilter;

    public UsersView(UserService userService) {
        this.userService = userService;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("User Management");
        add(title);

        createFilterControls();
        configureGrid();
        updateGrid(null);

        add(grid);
    }

    private void createFilterControls() {
        roleFilter = new ComboBox<>("Filter by Role");
        roleFilter.setItems(Role.values());
        roleFilter.setClearButtonVisible(true);

        addUserButton.addClickListener(e -> openUserForm(new User()));

        roleFilter.addValueChangeListener(e -> updateGrid(e.getValue()));

        HorizontalLayout controls = new HorizontalLayout(roleFilter, addUserButton);
        controls.setSpacing(true);

        add(controls);
    }

    private void configureGrid() {
        grid.setColumns("firstName", "lastName", "username", "email");

        grid.addColumn(User::getRole).setHeader("Role");

        grid.addComponentColumn(user -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editButton = new Button("Edit", e -> openUserForm(user));
            Button deleteButton = new Button("Delete", e -> {
                userService.deleteUser(user.getUserId());
                updateGrid(roleFilter.getValue());
                Notification.show("User deleted", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            });

            actions.add(editButton, deleteButton);
            return actions;
        }).setHeader("Actions");

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private void updateGrid(Role selectedRole) {
        List<User> users;

        if (selectedRole != null) {
            users = userService.getUsersByRole(selectedRole);
        } else {
            users = userService.getAllUsers();
        }

        grid.setItems(users);
    }

    private void openUserForm(User user) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();

        boolean isNewUser = user.getUserId() == null;
        H3 title = new H3(isNewUser ? "Add New User" : "Edit User");
        content.add(title);

        FormLayout formLayout = new FormLayout();

        Binder<User> binder = new Binder<>(User.class);

        TextField firstNameField = new TextField("First Name");
        TextField lastNameField = new TextField("Last Name");
        TextField usernameField = new TextField("Username");
        EmailField emailField = new EmailField("Email");
        PasswordField passwordField = new PasswordField("Password");
        ComboBox<User.Role> roleField = new ComboBox<>("Role");
        roleField.setItems(User.Role.values());

        binder.forField(firstNameField).asRequired("First name is required")
                .bind(User::getFirstName, User::setFirstName);

        binder.forField(lastNameField).asRequired("Last name is required")
                .bind(User::getLastName, User::setLastName);

        binder.forField(usernameField).asRequired("Username is required")
                .bind(User::getUsername, User::setUsername);

        binder.forField(emailField).asRequired("Email is required")
                .bind(User::getEmail, User::setEmail);

        // Only require password for new users
        if (isNewUser) {
            binder.forField(passwordField).asRequired("Password is required")
                    .bind(User::getPassword, User::setPassword);
        } else {
            binder.forField(passwordField)
                    .bind(
                            u -> "", // Don't show the actual password
                            (u, value) -> {
                                if (!value.isEmpty()) {
                                    u.setPassword(value);
                                }
                            });
        }

        binder.forField(roleField).asRequired("Role is required")
                .bind(User::getRole, User::setRole);

        binder.readBean(user);

        formLayout.add(
                firstNameField, lastNameField, usernameField,
                emailField, passwordField, roleField);

        content.add(formLayout);

        HorizontalLayout buttons = new HorizontalLayout();

        Button saveButton = new Button("Save", e -> {
            if (binder.validate().isOk()) {
                try {
                    binder.writeBean(user);
                    userService.saveUser(user);

                    Notification.show("User saved successfully", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                    dialog.close();
                    updateGrid(roleFilter.getValue());
                } catch (Exception ex) {
                    Notification.show("Error saving user: " + ex.getMessage(),
                            3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        buttons.add(saveButton, cancelButton);
        content.add(buttons);

        dialog.add(content);
        dialog.open();
    }
}