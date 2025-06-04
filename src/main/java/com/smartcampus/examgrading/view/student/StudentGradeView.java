package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Exam;
import com.smartcampus.examgrading.model.Grade;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.RevaluationRequest;
import com.smartcampus.examgrading.service.ExamService;
import com.smartcampus.examgrading.service.GradeService;
import com.smartcampus.examgrading.service.SessionService;
import com.smartcampus.examgrading.service.StudentService;
import com.smartcampus.examgrading.service.RevaluationService;
import com.smartcampus.examgrading.view.LoginView;
import com.smartcampus.examgrading.view.faculty.FacultyGradeView;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("student-grades")
@PageTitle("My Grades | Smart Campus System")
public class StudentGradeView extends VerticalLayout implements BeforeEnterObserver {

    private final GradeService gradeService;
    private final ExamService examService;
    private final SessionService sessionService;
    private final StudentService studentService;
    private final RevaluationService revaluationService;

    // Course selector
    private ComboBox<Course> courseComboBox = new ComboBox<>("Filter by Course");

    private final Grid<Grade> gradeGrid = new Grid<>(Grade.class, false);
    private final H3 courseStatsHeader = new H3("Course Performance");
    private final Paragraph courseStats = new Paragraph();
    private final H3 semesterGpaHeader = new H3("Semester Performance");
    private final Paragraph semesterStats = new Paragraph();

    private User currentUser;
    private List<Course> enrolledCourses;

    public StudentGradeView(GradeService gradeService, ExamService examService,
            SessionService sessionService, StudentService studentService,
            RevaluationService revaluationService) {
        this.gradeService = gradeService;
        this.examService = examService;
        this.sessionService = sessionService;
        this.studentService = studentService;
        this.revaluationService = revaluationService;

        // Set layout properties
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Check if user is logged in
        if (!sessionService.isLoggedIn()) {
            return; // BeforeEnter will handle the redirect
        }

        this.currentUser = sessionService.getCurrentUser();

        if (!sessionService.isStudent()) {
            // Display access message for non-students
            add(new H2("Access Information"),
                    new Paragraph(
                            "You are not logged in as a student. Faculty can use the Grade Management view to manage grades."));
            return;
        }

        setupLayout();
    }

    private void setupLayout() {
        // Header
        add(new H2("My Grades"));

        // Course filter
        courseComboBox.setWidthFull();
        courseComboBox.setItemLabelGenerator(course -> course.getCourseCode() + " - " + course.getCourseName());
        courseComboBox.addValueChangeListener(event -> {
            Course selectedCourse = event.getValue();
            filterGradesBySelectedCourse(selectedCourse);
        });

        // Add an "All Courses" option
        HorizontalLayout filterLayout = new HorizontalLayout(courseComboBox);
        filterLayout.setWidthFull();
        add(filterLayout);

        // Configure grade grid
        configureGradeGrid();
        add(gradeGrid);

        // Add statistics sections
        add(courseStatsHeader, courseStats);
        add(semesterGpaHeader, semesterStats);

        // Load data
        loadEnrolledCourses();
        updateGradeGrid();
    }

    private void loadEnrolledCourses() {
        if (currentUser != null) {
            enrolledCourses = studentService.getStudentCourses(currentUser.getUserId());

            // Setup the course combo box with enrolled courses
            courseComboBox.setItems(enrolledCourses);

            // Add "All Courses" option
            Course allCourses = new Course();
            allCourses.setCourseId(-1L); // Special ID for "All Courses"
            allCourses.setCourseCode("ALL");
            allCourses.setCourseName("All Courses");

            // Create a new list with "All Courses" at the beginning
            List<Course> courseOptions = new java.util.ArrayList<>();
            courseOptions.add(allCourses);
            courseOptions.addAll(enrolledCourses);

            courseComboBox.setItems(courseOptions);
            courseComboBox.setValue(allCourses); // Default to showing all courses
        }
    }

    private void configureGradeGrid() {
        // Course and Exam info
        gradeGrid.addColumn(grade -> {
            Exam exam = grade.getExam();
            Course course = exam != null && exam.getCourseId() != null
                    ? examService.getCourseById(exam.getCourseId()).orElse(null)
                    : null;
            return course != null ? course.getCourseCode() : "Unknown";
        }).setHeader("Course Code").setAutoWidth(true);

        gradeGrid.addColumn(grade -> {
            Exam exam = grade.getExam();
            Course course = exam != null && exam.getCourseId() != null
                    ? examService.getCourseById(exam.getCourseId()).orElse(null)
                    : null;
            return course != null ? course.getCourseName() : "Unknown Course";
        }).setHeader("Course Name").setAutoWidth(true);

        gradeGrid.addColumn(grade -> {
            Exam exam = grade.getExam();
            return exam != null ? exam.getExamName() : "Unknown Exam";
        }).setHeader("Exam").setAutoWidth(true);

        gradeGrid.addColumn(grade -> {
            Exam exam = grade.getExam();
            return exam != null ? exam.getExamType().toString() : "";
        }).setHeader("Type").setAutoWidth(true);

        // Grade details
        gradeGrid.addColumn(grade -> {
            Exam exam = grade.getExam();
            return exam != null ? exam.getTotalMarks().toString() : "N/A";
        }).setHeader("Total Marks").setAutoWidth(true);

        gradeGrid.addColumn(grade -> grade.getMarksObtained().toString()).setHeader("Marks Obtained")
                .setAutoWidth(true);
        gradeGrid.addColumn(grade -> grade.getPercentage().toString() + "%").setHeader("Percentage").setAutoWidth(true);
        gradeGrid.addColumn(Grade::getGradeLetter).setHeader("Grade").setAutoWidth(true);

        // Additional info
        gradeGrid.addColumn(Grade::getFeedback).setHeader("Feedback").setAutoWidth(true);

        gradeGrid.addColumn(grade -> {
            if (grade.getGradedAt() == null)
                return "N/A";
            return grade.getGradedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }).setHeader("Date").setAutoWidth(true);

        // Add revaluation request status and button
        gradeGrid.addColumn(new ComponentRenderer<>(grade -> {
            // Get revaluation requests for this grade
            List<RevaluationRequest> requests = revaluationService
                    .getStudentRevaluationRequests(currentUser.getUserId())
                    .stream()
                    .filter(request -> request.getGrade().getGradeId().equals(grade.getGradeId()))
                    .toList();

            if (requests.isEmpty()) {
                // No request exists, show request button
                Button requestBtn = new Button("Request Revaluation", new Icon(VaadinIcon.REFRESH));
                requestBtn.addClickListener(e -> openRevaluationDialog(grade));
                return requestBtn;
            } else {
                // Show status of the most recent request
                RevaluationRequest latestRequest = requests.get(requests.size() - 1);
                String status = latestRequest.getStatus().toString();
                Icon statusIcon;

                switch (status) {
                    case "PENDING":
                        statusIcon = new Icon(VaadinIcon.CLOCK);
                        statusIcon.setColor("var(--lumo-primary-color)");
                        break;
                    case "APPROVED":
                        statusIcon = new Icon(VaadinIcon.CHECK);
                        statusIcon.setColor("var(--lumo-success-color)");
                        break;
                    case "REJECTED":
                        statusIcon = new Icon(VaadinIcon.CLOSE);
                        statusIcon.setColor("var(--lumo-error-color)");
                        break;
                    default:
                        statusIcon = new Icon(VaadinIcon.QUESTION);
                }

                HorizontalLayout statusLayout = new HorizontalLayout(statusIcon, new Span(status));
                statusLayout.setAlignItems(Alignment.CENTER);
                statusLayout.setSpacing(true);
                return statusLayout;
            }
        })).setHeader("Revaluation Status").setAutoWidth(true);

        gradeGrid.setHeight("400px");
    }

    private void updateGradeGrid() {
        if (currentUser != null) {
            List<Grade> grades = gradeService.getGradesByStudentId(currentUser.getUserId());

            // Set all grades and hydrate with course and exam information
            for (Grade grade : grades) {
                // Load exam data for each grade
                examService.getExamById(grade.getExamId()).ifPresent(grade::setExam);
            }

            gradeGrid.setItems(grades);

            // Calculate and display semester statistics
            updateSemesterStatistics(grades);
        }
    }

    private void filterGradesBySelectedCourse(Course selectedCourse) {
        if (currentUser != null) {
            List<Grade> allGrades = gradeService.getGradesByStudentId(currentUser.getUserId());

            // Hydrate grades with exam data
            for (Grade grade : allGrades) {
                examService.getExamById(grade.getExamId()).ifPresent(grade::setExam);
            }

            List<Grade> filteredGrades;

            if (selectedCourse == null || selectedCourse.getCourseId() == -1L) {
                // Show all grades if no course is selected or "All Courses" is selected
                filteredGrades = allGrades;
                updateSemesterStatistics(allGrades);
                courseStatsHeader.setText("Overall Performance");
            } else {
                // Filter grades by selected course
                filteredGrades = allGrades.stream()
                        .filter(grade -> {
                            Exam exam = grade.getExam();
                            return exam != null && exam.getCourseId().equals(selectedCourse.getCourseId());
                        })
                        .collect(Collectors.toList());

                updateCourseStatistics(filteredGrades, selectedCourse);
                courseStatsHeader.setText(selectedCourse.getCourseCode() + " - " +
                        selectedCourse.getCourseName() + " Performance");
            }

            gradeGrid.setItems(filteredGrades);
        }
    }

    private void updateCourseStatistics(List<Grade> grades, Course course) {
        if (grades.isEmpty()) {
            courseStats.setText("No grades available for this course yet.");
            return;
        }

        // Calculate average percentage for this course
        double averagePercentage = grades.stream()
                .mapToDouble(g -> g.getPercentage().doubleValue())
                .average()
                .orElse(0.0);

        // Count grades by letter for this course
        Map<String, Long> gradeDistribution = grades.stream()
                .collect(Collectors.groupingBy(Grade::getGradeLetter, Collectors.counting()));

        // Format the statistics text
        StringBuilder statsText = new StringBuilder();
        statsText.append("Course average: ").append(String.format("%.2f%%", averagePercentage)).append("\n");
        statsText.append("Grade distribution in this course:\n");

        // Sort grade letters
        List<String> sortedGradeLetters = gradeDistribution.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (String gradeLetter : sortedGradeLetters) {
            statsText.append(gradeLetter).append(": ").append(gradeDistribution.get(gradeLetter)).append("\n");
        }

        courseStats.setText(statsText.toString());
    }

    private void updateSemesterStatistics(List<Grade> grades) {
        if (grades.isEmpty()) {
            semesterStats.setText("No grades available yet.");
            return;
        }

        // Calculate average percentage
        double averagePercentage = grades.stream()
                .mapToDouble(g -> g.getPercentage().doubleValue())
                .average()
                .orElse(0.0);

        // Count grades by letter
        Map<String, Long> gradeDistribution = grades.stream()
                .collect(Collectors.groupingBy(Grade::getGradeLetter, Collectors.counting()));

        // Calculate GPA
        double gpa = calculateGPA(grades);

        // Format the statistics text
        StringBuilder statsText = new StringBuilder();
        statsText.append("Overall average: ").append(String.format("%.2f%%", averagePercentage)).append("\n");
        statsText.append("GPA: ").append(String.format("%.2f", gpa)).append("\n\n");
        statsText.append("Overall grade distribution:\n");

        // Sort grade letters by traditional order (A, B, C, D, F)
        List<String> sortedGradeLetters = gradeDistribution.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (String gradeLetter : sortedGradeLetters) {
            statsText.append(gradeLetter).append(": ").append(gradeDistribution.get(gradeLetter)).append("\n");
        }

        semesterStats.setText(statsText.toString());
    }

    private double calculateGPA(List<Grade> grades) {
        // Simple GPA calculation - can be customized based on specific grading system
        double totalPoints = 0;
        int totalCount = 0;

        for (Grade grade : grades) {
            String letter = grade.getGradeLetter();
            double points = 0;

            // Standard 4.0 scale - adjust as needed
            switch (letter) {
                case "A+":
                    points = 4.0;
                    break;
                case "A":
                    points = 4.0;
                    break;
                case "A-":
                    points = 3.7;
                    break;
                case "B+":
                    points = 3.3;
                    break;
                case "B":
                    points = 3.0;
                    break;
                case "B-":
                    points = 2.7;
                    break;
                case "C+":
                    points = 2.3;
                    break;
                case "C":
                    points = 2.0;
                    break;
                case "C-":
                    points = 1.7;
                    break;
                case "D+":
                    points = 1.3;
                    break;
                case "D":
                    points = 1.0;
                    break;
                case "F":
                    points = 0.0;
                    break;
                default:
                    continue; // Skip grades like "I" (incomplete) or "W" (withdrawn)
            }

            totalPoints += points;
            totalCount++;
        }

        return totalCount > 0 ? totalPoints / totalCount : 0.0;
    }

    private void openRevaluationDialog(Grade grade) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Request Revaluation");

        // Form layout
        FormLayout formLayout = new FormLayout();

        // Reason text area
        TextArea reasonField = new TextArea("Reason for Revaluation");
        reasonField.setWidthFull();
        reasonField.setHeight("150px");
        reasonField.setRequired(true);

        formLayout.add(reasonField);
        dialog.add(formLayout);

        // Buttons
        Button submitButton = new Button("Submit Request", e -> {
            if (reasonField.isEmpty()) {
                Notification.show("Please provide a reason for revaluation", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                revaluationService.createRevaluationRequest(grade.getGradeId(), reasonField.getValue());
                Notification
                        .show("Revaluation request submitted successfully", 3000, Notification.Position.BOTTOM_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Error submitting request: " + ex.getMessage(),
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelButton, submitButton);
        dialog.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Redirect to login if not logged in
        if (!sessionService.isLoggedIn()) {
            event.forwardTo(LoginView.class);
            return;
        }

        // Redirect non-students to the appropriate view
        if (!sessionService.isStudent()) {
            event.forwardTo(FacultyGradeView.class);
        }
    }
}