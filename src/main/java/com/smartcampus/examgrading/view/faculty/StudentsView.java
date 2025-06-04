package com.smartcampus.examgrading.view.faculty;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;

import java.util.List;
import java.util.stream.Collectors;

@Route(value = "faculty/students", layout = FacultyView.class)
@PageTitle("Manage Students | Faculty")
public class StudentsView extends VerticalLayout {
    private final Grid<User> grid = new Grid<>(User.class);
    private final CourseService courseService;
    private final SecurityService securityService;
    private ComboBox<Course> courseFilter;

    public StudentsView(CourseService courseService, SecurityService securityService) {
        this.courseService = courseService;
        this.securityService = securityService;

        try {
            if (!securityService.isLoggedIn() || !securityService.hasRole(User.Role.FACULTY)) {
                getUI().ifPresent(ui -> ui.navigate(""));
                return;
            }

            setSizeFull();
            setSpacing(true);
            setPadding(true);

            add(new H2("Manage Students"));
            add(new Paragraph("View students enrolled in your courses and manage their access"));

            createFilterControls();
            configureGrid();
            updateGrid(null);

            add(grid);
        } catch (Exception e) {
            Notification.show("Error initializing view: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void createFilterControls() {
        try {
            User faculty = securityService.getCurrentUser();
            List<Course> courses = courseService.getCoursesByFaculty(faculty);

            courseFilter = new ComboBox<>("Filter by Course");
            courseFilter.setItems(courses);
            courseFilter.setItemLabelGenerator(Course::getCourseName);
            courseFilter.setClearButtonVisible(true);

            courseFilter.addValueChangeListener(e -> updateGrid(e.getValue()));

            add(courseFilter);
        } catch (Exception e) {
            Notification.show("Error loading courses: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void configureGrid() {
        try {
            grid.setSizeFull();
            grid.setColumns();

            grid.addColumn(User::getFirstName).setHeader("First Name").setSortable(true);
            grid.addColumn(User::getLastName).setHeader("Last Name").setSortable(true);
            grid.addColumn(User::getEmail).setHeader("Email").setSortable(true);
            grid.addColumn(student -> {
                try {
                    List<Course> courses = courseService.getStudentEnrollments(student).stream()
                            .map(Enrollment::getCourse)
                            .filter(course -> {
                                User currentFaculty = securityService.getCurrentUser();
                                User courseFaculty = course.getFaculty();
                                return courseFaculty != null && currentFaculty != null &&
                                        courseFaculty.getUserId().equals(currentFaculty.getUserId());
                            })
                            .collect(Collectors.toList());
                    return courses.stream()
                            .map(Course::getCourseCode)
                            .collect(Collectors.joining(", "));
                } catch (Exception e) {
                    return "Error loading courses";
                }
            }).setHeader("Enrolled Courses").setSortable(true);

            grid.addComponentColumn(student -> {
                HorizontalLayout buttonsLayout = new HorizontalLayout();

                try {
                    List<Course> coursesWithAccess = courseService.getStudentEnrollments(student).stream()
                            .map(Enrollment::getCourse)
                            .filter(course -> {
                                User currentFaculty = securityService.getCurrentUser();
                                User courseFaculty = course.getFaculty();
                                return courseFaculty != null && currentFaculty != null &&
                                        courseFaculty.getUserId().equals(currentFaculty.getUserId());
                            })
                            .collect(Collectors.toList());

                    if (!coursesWithAccess.isEmpty()) {
                        buttonsLayout.add(createManageAccessButton(student, coursesWithAccess));
                    }
                } catch (Exception e) {
                    Notification.show("Error loading student access: " + e.getMessage(),
                            3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }

                return buttonsLayout;
            }).setHeader("Actions");

            grid.getColumns().forEach(col -> col.setAutoWidth(true));
        } catch (Exception e) {
            Notification.show("Error configuring grid: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateGrid(Course selectedCourse) {
        try {
            User faculty = securityService.getCurrentUser();
            List<User> students;

            if (selectedCourse != null) {
                students = courseService.getEnrollmentsForCourse(selectedCourse).stream()
                        .map(Enrollment::getStudent)
                        .collect(Collectors.toList());
            } else {
                students = courseService.getCoursesByFaculty(faculty).stream()
                        .flatMap(course -> courseService.getEnrollmentsForCourse(course).stream())
                        .map(Enrollment::getStudent)
                        .distinct()
                        .collect(Collectors.toList());
            }

            grid.setItems(students);
        } catch (Exception e) {
            Notification.show("Error loading students: " + e.getMessage(),
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private com.vaadin.flow.component.button.Button createManageAccessButton(User student, List<Course> courses) {
        return new com.vaadin.flow.component.button.Button("Manage Access", e -> manageAccess(student, courses));
    }

    private void manageAccess(User student, List<Course> courses) {
        Dialog dialog = new Dialog();
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.add(new H3("Manage Course Access for " + student.getFirstName() + " " + student.getLastName()));

        Grid<Course> coursesGrid = new Grid<>(Course.class);
        coursesGrid.setItems(courses);
        coursesGrid.setColumns("courseCode", "courseName", "creditHours");

        content.add(coursesGrid);

        Button closeButton = new Button("Close", e -> dialog.close());
        content.add(closeButton);

        dialog.add(content);
        dialog.open();
    }

    // Helper class for managing course access
    private static class CourseAccess {
        private final Long courseId;
        private final String courseTitle;
        private boolean hasAccess;

        public CourseAccess(Long courseId, String courseTitle, boolean hasAccess) {
            this.courseId = courseId;
            this.courseTitle = courseTitle;
            this.hasAccess = hasAccess;
        }

        public Long getCourseId() {
            return courseId;
        }

        public String getCourseTitle() {
            return courseTitle;
        }

        public boolean isHasAccess() {
            return hasAccess;
        }

        public void setHasAccess(boolean hasAccess) {
            this.hasAccess = hasAccess;
        }
    }
}