package com.smartcampus.examgrading.view.admin;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route(value = "admin/courses", layout = MainLayout.class)
@PageTitle("Course Management | Admin")
public class CoursesManagementView extends VerticalLayout {
    private final Grid<Course> grid = new Grid<>(Course.class);
    private final CourseService courseService;
    private final UserService userService;
    private final SecurityService securityService;

    private final Button addCourseButton = new Button("Add Course");

    public CoursesManagementView(CourseService courseService, UserService userService,
            SecurityService securityService) {
        this.courseService = courseService;
        this.userService = userService;
        this.securityService = securityService;

        try {
            if (!securityService.isLoggedIn() || !securityService.hasRole(User.Role.ADMIN)) {
                getUI().ifPresent(ui -> ui.navigate(""));
                return;
            }

            setSizeFull();
            setSpacing(true);
            setPadding(true);

            H2 title = new H2("Course Management");
            add(title);

            addCourseButton.addClickListener(e -> openCourseForm(new Course()));

            HorizontalLayout toolBar = new HorizontalLayout(addCourseButton);
            add(toolBar);

            configureGrid();
            updateGrid();

            add(grid);
        } catch (Exception e) {
            Notification.show("Error initializing view: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void configureGrid() {
        try {
            grid.setSizeFull();
            grid.setColumns();

            grid.addColumn(Course::getCourseCode).setHeader("Course Code").setAutoWidth(true);
            grid.addColumn(Course::getCourseName).setHeader("Course Name").setAutoWidth(true);
            grid.addColumn(Course::getContent).setHeader("Description").setAutoWidth(true);

            grid.addColumn(course -> {
                try {
                    User faculty = course.getFaculty();
                    return faculty == null ? "Not assigned" : faculty.getFirstName() + " " + faculty.getLastName();
                } catch (Exception e) {
                    return "Error loading faculty";
                }
            }).setHeader("Faculty").setAutoWidth(true);

            grid.addColumn(Course::getCreditHours).setHeader("Credit Hours").setAutoWidth(true);
            grid.addColumn(Course::getCapacity).setHeader("Capacity").setAutoWidth(true);
            grid.addColumn(course -> {
                try {
                    return course.getCurrentEnrollmentCount() + " / " + course.getCapacity();
                } catch (Exception e) {
                    return "Error";
                }
            }).setHeader("Enrollment").setAutoWidth(true);

            grid.addComponentColumn(course -> {
                Button editButton = new Button("Edit");
                editButton.addClickListener(e -> openCourseForm(course));
                return editButton;
            }).setHeader("Actions").setAutoWidth(true);

        } catch (Exception e) {
            Notification.show("Error configuring grid: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateGrid() {
        try {
            List<Course> courses = courseService.getAllCourses();
            grid.setItems(courses);
        } catch (Exception e) {
            Notification.show("Error updating grid: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openCourseForm(Course course) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setModal(true);

        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(true);

        H3 title = new H3(course.getId() == null ? "Add New Course" : "Edit Course");
        mainLayout.add(title);

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();

        // Create form fields
        TextField courseCodeField = new TextField("Course Code");
        courseCodeField.setRequired(true);
        courseCodeField.setWidthFull();

        TextField courseNameField = new TextField("Course Name");
        courseNameField.setRequired(true);
        courseNameField.setWidthFull();

        TextArea contentField = new TextArea("Description");
        contentField.setWidthFull();

        IntegerField creditHoursField = new IntegerField("Credit Hours");
        creditHoursField.setRequired(true);
        creditHoursField.setMin(1);
        creditHoursField.setValue(3); // Default value

        IntegerField capacityField = new IntegerField("Capacity");
        capacityField.setRequired(true);
        capacityField.setMin(1);
        capacityField.setValue(30); // Default value

        ComboBox<User> facultyComboBox = new ComboBox<>("Faculty");
        facultyComboBox.setRequired(true);
        facultyComboBox.setWidthFull();

        // Load faculty list
        List<User> facultyList = userService.getUsersByRole(User.Role.FACULTY);
        facultyComboBox.setItems(facultyList);
        facultyComboBox.setItemLabelGenerator(user -> user.getFirstName() + " " + user.getLastName());

        // Add fields to form
        formLayout.add(
            courseCodeField, courseNameField, contentField,
            creditHoursField, capacityField, facultyComboBox
        );

        // Set up binder
        Binder<Course> binder = new Binder<>(Course.class);

        // Bind fields with validation
        binder.forField(courseCodeField)
            .asRequired("Course code is required")
            .bind(Course::getCourseCode, Course::setCourseCode);

        binder.forField(courseNameField)
            .asRequired("Course name is required")
            .bind(Course::getCourseName, Course::setCourseName);

        binder.forField(contentField)
            .bind(Course::getContent, Course::setContent);

        binder.forField(creditHoursField)
            .asRequired("Credit hours is required")
            .withValidator(credits -> credits >= 1, "Credit hours must be at least 1")
            .bind(Course::getCreditHours, Course::setCreditHours);

        binder.forField(capacityField)
            .asRequired("Capacity is required")
            .withValidator(cap -> cap >= 1, "Capacity must be at least 1")
            .bind(Course::getCapacity, Course::setCapacity);

        binder.forField(facultyComboBox)
            .asRequired("Faculty is required")
            .bind(Course::getFaculty, Course::setFaculty);

        // Read the course data if editing
        if (course.getId() != null) {
            binder.readBean(course);
        }

        // Create buttons
        Button saveButton = new Button("Save", e -> {
            try {
                if (binder.validate().isOk()) {
                    binder.writeBean(course);
                    courseService.saveCourse(course);
                    dialog.close();
                    updateGrid();
                    Notification.show("Course saved successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            } catch (Exception ex) {
                Notification.show("Error saving course: " + ex.getMessage(), 
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setPadding(true);

        mainLayout.add(formLayout, buttonLayout);
        dialog.add(mainLayout);
        dialog.open();
    }
}