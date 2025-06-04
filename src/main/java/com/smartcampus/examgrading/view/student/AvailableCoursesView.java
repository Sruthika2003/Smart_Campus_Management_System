package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Route(value = "student/available-courses", layout = StudentView.class)
@PageTitle("Available Courses | Course Management System")
public class AvailableCoursesView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(AvailableCoursesView.class);
    private final Grid<Course> grid = new Grid<>(Course.class);
    private final CourseService courseService;
    private final SecurityService securityService;

    public AvailableCoursesView(CourseService courseService, SecurityService securityService) {
        this.courseService = courseService;
        this.securityService = securityService;

        try {
            if (!securityService.isLoggedIn() || !securityService.hasRole(User.Role.STUDENT)) {
                getUI().ifPresent(ui -> ui.navigate(""));
                return;
            }

            setSizeFull();
            setSpacing(true);
            setPadding(true);

            add(new H2("Available Courses"));
            add(new Paragraph("Browse and register for available courses"));

            configureGrid();
            updateGrid();

            add(grid);
        } catch (Exception e) {
            logger.error("Error initializing AvailableCoursesView", e);
            showNotification("Error initializing view: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }

    private void configureGrid() {
        try {
            grid.setSizeFull();
            grid.setColumns();

            grid.addColumn(Course::getCourseCode)
                    .setHeader("Course Code")
                    .setSortable(true);

            grid.addColumn(Course::getCourseName)
                    .setHeader("Title")
                    .setSortable(true);

            grid.addColumn(course -> {
                try {
                    User faculty = course.getFaculty();
                    return faculty != null ? faculty.getFirstName() + " " + faculty.getLastName() : "Not Assigned";
                } catch (Exception e) {
                    logger.error("Error getting faculty for course: " + course.getCourseCode(), e);
                    return "Error loading faculty";
                }
            })
                    .setHeader("Faculty")
                    .setSortable(true);

            grid.addColumn(Course::getCreditHours)
                    .setHeader("Credits")
                    .setSortable(true);

            grid.addColumn(course -> {
                try {
                    int current = course.getCurrentEnrollmentCount();
                    int max = course.getCapacity();
                    return String.format("%d / %d", current, max);
                } catch (Exception e) {
                    logger.error("Error getting enrollment count for course: " + course.getCourseCode(), e);
                    return "Error";
                }
            })
                    .setHeader("Enrollment")
                    .setSortable(true);

            grid.addComponentColumn(course -> {
                Button registerButton = new Button("Register", e -> registerForCourse(course));
                registerButton.setEnabled(course.hasAvailableSeats());
                return registerButton;
            }).setHeader("Action");

            grid.getColumns().forEach(col -> col.setAutoWidth(true));
        } catch (Exception e) {
            logger.error("Error configuring grid", e);
            showNotification("Error configuring course list: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateGrid() {
        try {
            List<Course> courses = courseService.getCoursesWithAvailableSeats();
            logger.info("Found {} available courses", courses.size());
            grid.setItems(courses);
        } catch (Exception e) {
            logger.error("Error loading courses", e);
            showNotification("Error loading courses: " + e.getMessage(),
                    NotificationVariant.LUMO_ERROR);
            grid.setItems(Collections.emptyList());
        }
    }

    private void registerForCourse(Course course) {
        try {
            Enrollment enrollment = courseService.enrollStudentInCourse(
                    securityService.getCurrentUser(), course);

            if (enrollment != null) {
                showNotification("Successfully registered for " + course.getCourseName(),
                        NotificationVariant.LUMO_SUCCESS);
                updateGrid();
            }
        } catch (Exception e) {
            logger.error("Error registering for course: " + course.getCourseCode(), e);
            showNotification(e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void showNotification(String message, NotificationVariant variant) {
        getUI().ifPresent(ui -> ui.access(() -> {
            Notification notification = Notification.show(message, 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(variant);
        }));
    }
}