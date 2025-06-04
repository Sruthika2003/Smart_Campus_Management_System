package com.smartcampus.examgrading.view.faculty;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.Exam;
import com.smartcampus.examgrading.model.Exam.ExamType;
import com.smartcampus.examgrading.service.ExamService;
import com.smartcampus.examgrading.service.SessionService;
import com.smartcampus.examgrading.view.LoginView;
import com.smartcampus.examgrading.view.MainLayout;
import com.smartcampus.examgrading.view.student.StudentExamScheduleView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;

import java.sql.Date;
import java.sql.Time;
import java.math.BigDecimal;

@Route(value = "faculty/exams", layout = MainLayout.class)
@PageTitle("Exam Management | Smart Campus System")
public class ExamView extends VerticalLayout implements BeforeEnterObserver {

    private final ExamService examService;
    private final SessionService sessionService;
    private final com.vaadin.flow.component.grid.Grid<Exam> grid = new com.vaadin.flow.component.grid.Grid<>(
            Exam.class);

    private TextField examName = new TextField("Exam Name");
    private ComboBox<Course> courseComboBox = new ComboBox<>("Course");

    private ComboBox<ExamType> examType = new ComboBox<>("Exam Type");
    private com.vaadin.flow.component.datepicker.DatePicker examDate = new com.vaadin.flow.component.datepicker.DatePicker(
            "Exam Date");
    private com.vaadin.flow.component.timepicker.TimePicker startTime = new com.vaadin.flow.component.timepicker.TimePicker(
            "Start Time");
    private com.vaadin.flow.component.timepicker.TimePicker endTime = new com.vaadin.flow.component.timepicker.TimePicker(
            "End Time");
    private NumberField totalMarks = new NumberField("Total Marks");
    private NumberField passingMarks = new NumberField("Passing Marks");
    private TextArea examInstructions = new TextArea("Instructions");

    private Button saveBtn = new Button("Schedule Exam");

    public ExamView(ExamService examService, SessionService sessionService) {
        this.examService = examService;
        this.sessionService = sessionService;

        // Check if user is logged in and has appropriate role
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null
                || (currentUser.getRole() != User.Role.FACULTY && currentUser.getRole() != User.Role.ADMIN)) {
            // Display access denied message
            add(new H2("Access Denied"),
                    new Paragraph("Only faculty members and administrators can schedule exams."));
            return;
        }

        // Initialize form components
        initializeForm();

        // Add form and grid to layout
        com.vaadin.flow.component.formlayout.FormLayout formLayout = new com.vaadin.flow.component.formlayout.FormLayout(
                examName, courseComboBox, examType,
                examDate, startTime, endTime,
                totalMarks, passingMarks, examInstructions,
                saveBtn);

        add(new H2("Schedule New Exam"), formLayout, new H2("Existing Exams"), grid);

        // Load data
        updateGrid();
    }

    private void initializeForm() {
        // Set up course dropdown
        courseComboBox.setItems(examService.getAllCourses());
        courseComboBox.setItemLabelGenerator(Course::getCourseName);

        // Set up exam type dropdown
        examType.setItems(ExamType.values());

        // Configure grid
        grid.setColumns("id", "examName");
        grid.addColumn(exam -> {
            Course course = examService.getCourseById(exam.getCourseId()).orElse(null);
            return course != null ? course.getCourseName() : "";
        }).setHeader("Course");
        grid.addColumn("examType").setHeader("Type");
        grid.addColumn("examDate").setHeader("Date");
        grid.addColumn("startTime").setHeader("Start Time");
        grid.addColumn("endTime").setHeader("End Time");
        grid.addColumn("totalMarks").setHeader("Total Marks");
        grid.addColumn("passingMarks").setHeader("Passing Marks");

        // Save button action
        saveBtn.addClickListener(e -> saveExam());
    }

    private void saveExam() {
        // Validate form
        if (!validateForm()) {
            Notification.show("Please fill all required fields", 3000, Notification.Position.BOTTOM_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Exam exam = new Exam();
        exam.setExamName(examName.getValue());
        exam.setCourseId(courseComboBox.getValue().getCourseId());
        exam.setExamType(examType.getValue());
        exam.setExamDate(Date.valueOf(examDate.getValue()));
        exam.setStartTime(Time.valueOf(startTime.getValue()));
        exam.setEndTime(Time.valueOf(endTime.getValue()));
        exam.setTotalMarks(BigDecimal.valueOf(totalMarks.getValue()));
        exam.setPassingMarks(BigDecimal.valueOf(passingMarks.getValue()));
        exam.setExamInstructions(examInstructions.getValue());
        exam.setCreatedBy(sessionService.getCurrentUser().getUserId());

        examService.saveExam(exam);

        // Clear form and update grid
        clearForm();
        updateGrid();

        Notification.show("Exam scheduled successfully", 3000, Notification.Position.BOTTOM_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private boolean validateForm() {
        return !examName.isEmpty() &&
                courseComboBox.getValue() != null &&
                examType.getValue() != null &&
                examDate.getValue() != null &&
                startTime.getValue() != null &&
                endTime.getValue() != null &&
                totalMarks.getValue() != null &&
                passingMarks.getValue() != null;
    }

    private void updateGrid() {
        grid.setItems(examService.getAllExams());
    }

    private void clearForm() {
        examName.clear();
        courseComboBox.clear();
        examType.clear();
        examDate.clear();
        startTime.clear();
        endTime.clear();
        totalMarks.clear();
        passingMarks.clear();
        examInstructions.clear();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Redirect to login if not logged in
        if (!sessionService.isLoggedIn()) {
            event.getUI().navigate(LoginView.class);
            return;
        }

        // Redirect students to the student exam schedule view
        User currentUser = sessionService.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.Role.STUDENT) {
            event.getUI().navigate(StudentExamScheduleView.class);
        }
    }
}