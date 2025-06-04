package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Exam;
import com.smartcampus.examgrading.model.Grade;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.ExamService;
import com.smartcampus.examgrading.service.GradeService;
import com.smartcampus.examgrading.service.SessionService;
import com.smartcampus.examgrading.view.LoginView;
import com.smartcampus.examgrading.view.faculty.FacultyGradeView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Route("student-exam-result/:examId?")
@PageTitle("Exam Result | Smart Campus System")
public class StudentExamResultView extends VerticalLayout implements BeforeEnterObserver {

    private final GradeService gradeService;
    private final ExamService examService;
    private final SessionService sessionService;

    private ComboBox<Exam> examSelector;
    private VerticalLayout resultContainer;
    private User currentUser;
    private Long examId;

    public StudentExamResultView(GradeService gradeService, ExamService examService, SessionService sessionService) {
        this.gradeService = gradeService;
        this.examService = examService;
        this.sessionService = sessionService;

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
                    new Paragraph("You are not logged in as a student. Faculty can use the Grade Management view."));
            return;
        }

        setupLayout();
    }

    private void setupLayout() {
        // Back button
        Button backButton = new Button("Back to Grades", new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addClickListener(e -> UI.getCurrent().navigate(StudentGradeView.class));

        // Header
        H2 header = new H2("Exam Result Details");

        // Exam selector for viewing different results
        examSelector = new ComboBox<>("Select Exam");
        examSelector.setWidthFull();
        examSelector.setItemLabelGenerator(exam -> {
            Course course = examService.getCourseById(exam.getCourseId()).orElse(null);
            String courseName = course != null ? course.getCourseName() : "Unknown Course";
            return courseName + " - " + exam.getExamName();
        });
        examSelector.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                examId = event.getValue().getId();
                updateResultView();
            }
        });

        // Results container
        resultContainer = new VerticalLayout();
        resultContainer.setPadding(true);
        resultContainer.setSpacing(true);
        resultContainer.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");

        add(backButton, header, examSelector, resultContainer);

        // Load exams for this student
        loadStudentExams();
    }

    private void loadStudentExams() {
        if (currentUser != null) {
            // Get grades for this student to determine which exams they have taken
            List<Grade> grades = gradeService.getGradesByStudentId(currentUser.getUserId());

            // Extract exams from grades
            List<Exam> exams = grades.stream()
                    .map(Grade::getExam)
                    .filter(exam -> exam != null)
                    .distinct()
                    .toList();

            examSelector.setItems(exams);

            // If examId is set from URL parameter, select that exam
            if (examId != null) {
                exams.stream()
                        .filter(e -> e.getId().equals(examId))
                        .findFirst()
                        .ifPresent(examSelector::setValue);
            } else if (!exams.isEmpty()) {
                // Otherwise select the first exam
                examSelector.setValue(exams.get(0));
            }
        }
    }

    private void updateResultView() {
        resultContainer.removeAll();

        if (examId == null) {
            resultContainer.add(new Paragraph("No exam selected."));
            return;
        }

        // Get the exam
        Optional<Exam> examOpt = examService.getExamById(examId);
        if (examOpt.isEmpty()) {
            resultContainer.add(new Paragraph("Exam not found."));
            return;
        }

        Exam exam = examOpt.get();

        // Get the grade for this student and exam
        Optional<Grade> gradeOpt = gradeService.getGradeByStudentAndExam(currentUser.getUserId(), examId);
        if (gradeOpt.isEmpty()) {
            resultContainer.add(new Paragraph("No grade found for this exam."));
            return;
        }

        Grade grade = gradeOpt.get();

        // Get course information
        Course course = examService.getCourseById(exam.getCourseId()).orElse(null);
        String courseName = course != null ? course.getCourseName() : "Unknown Course";

        // Display exam and grade information
        H3 examName = new H3(courseName + " - " + exam.getExamName());

        // Create a grid for displaying exam details
        Div detailsGrid = new Div();
        detailsGrid.getStyle().set("display", "grid")
                .set("grid-template-columns", "auto auto")
                .set("gap", "10px")
                .set("margin-bottom", "20px");

        // Add exam details
        addDetailRow(detailsGrid, "Exam Type:", exam.getExamType().toString());
        addDetailRow(detailsGrid, "Total Marks:", exam.getTotalMarks().toString());
        addDetailRow(detailsGrid, "Your Score:", grade.getMarksObtained().toString());
        addDetailRow(detailsGrid, "Percentage:", grade.getPercentage().toString() + "%");
        addDetailRow(detailsGrid, "Grade:", grade.getGradeLetter());
        addDetailRow(detailsGrid, "Date Graded:",
                grade.getGradedAt() != null
                        ? grade.getGradedAt().toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        : "N/A");

        // Create grade display
        HorizontalLayout gradeDisplay = new HorizontalLayout();
        gradeDisplay.setWidthFull();
        gradeDisplay.setPadding(true);
        gradeDisplay.setSpacing(true);
        gradeDisplay.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        gradeDisplay.getStyle()
                .set("background-color", getGradeColor(grade.getGradeLetter()))
                .set("border-radius", "8px")
                .set("margin", "20px 0");

        Span gradeLabel = new Span("Your Grade:");
        gradeLabel.getStyle().set("font-size", "20px").set("font-weight", "bold").set("color", "white");

        Span gradeValue = new Span(grade.getGradeLetter());
        gradeValue.getStyle().set("font-size", "36px").set("font-weight", "bold").set("color", "white");

        gradeDisplay.add(gradeLabel, gradeValue);

        // Feedback section
        VerticalLayout feedbackLayout = new VerticalLayout();
        feedbackLayout.setPadding(true);
        feedbackLayout.getStyle()
                .set("background-color", "#f9f9f9")
                .set("border-radius", "8px")
                .set("border-left", "4px solid #1565C0");

        H3 feedbackHeader = new H3("Instructor Feedback");
        Paragraph feedbackText = new Paragraph(
                grade.getFeedback() != null && !grade.getFeedback().isBlank() ? grade.getFeedback()
                        : "No feedback provided.");

        feedbackLayout.add(feedbackHeader, feedbackText);

        // Class average information if available (placeholder for now)
        VerticalLayout classAverageLayout = new VerticalLayout();
        classAverageLayout.setPadding(true);
        classAverageLayout.getStyle()
                .set("background-color", "#f9f9f9")
                .set("border-radius", "8px")
                .set("border-left", "4px solid #4CAF50");

        H3 classAvgHeader = new H3("Class Performance");
        Paragraph classAvgText = new Paragraph("Class Average: Not available");

        classAverageLayout.add(classAvgHeader, classAvgText);

        // Add all components to result container
        resultContainer.add(examName, detailsGrid, gradeDisplay, feedbackLayout, classAverageLayout);
    }

    private void addDetailRow(Div grid, String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.getStyle().set("font-weight", "bold");

        Span valueSpan = new Span(value);

        grid.add(labelSpan, valueSpan);
    }

    private String getGradeColor(String gradeLetter) {
        // Return color based on grade letter
        switch (gradeLetter.charAt(0)) {
            case 'A':
                return "#4CAF50"; // Green
            case 'B':
                return "#2196F3"; // Blue
            case 'C':
                return "#FF9800"; // Orange
            case 'D':
                return "#FF5722"; // Dark Orange
            case 'F':
                return "#F44336"; // Red
            default:
                return "#9E9E9E"; // Grey
        }
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
            return;
        }

        // Get exam ID from URL if present
        String examIdParam = event.getRouteParameters().get("examId").orElse(null);
        if (examIdParam != null) {
            try {
                this.examId = Long.parseLong(examIdParam);
            } catch (NumberFormatException e) {
                // Invalid ID format, just continue without setting examId
            }
        }
    }
}