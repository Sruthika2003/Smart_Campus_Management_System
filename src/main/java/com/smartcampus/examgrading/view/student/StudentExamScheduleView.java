package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Exam;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.service.ExamService;
import com.smartcampus.examgrading.service.SessionService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.view.LoginView;
import com.smartcampus.examgrading.view.admin.ExamView;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;

import java.util.List;
import java.util.stream.Collectors;

@Route("student-exams")
@PageTitle("My Exam Schedule")
public class StudentExamScheduleView extends VerticalLayout implements BeforeEnterObserver {

    private final ExamService examService;
    private final SessionService sessionService;
    private final CourseService courseService;
    private final Grid<Exam> grid = new Grid<>(Exam.class);

    private User currentUser;

    public StudentExamScheduleView(ExamService examService, SessionService sessionService,
            CourseService courseService) {
        this.examService = examService;
        this.sessionService = sessionService;
        this.courseService = courseService;

        // Set layout properties
        setSizeFull();
        setPadding(true);

        // Check if user is logged in and is a student
        if (!sessionService.isLoggedIn()) {
            return; // BeforeEnter will handle the redirect
        }

        this.currentUser = sessionService.getCurrentUser();

        if (!sessionService.isStudent()) {
            // Display access message for non-students
            add(new H2("Access Information"),
                    new Paragraph(
                            "You are not logged in as a student. Faculty and administrators can use the Exam Management view to schedule exams."));
            return;
        }

        // Set up the grid
        configureGrid();

        // Add components to layout
        add(new H2("My Exam Schedule"), grid);

        // Load student's exam data
        updateGrid();
    }

    private void configureGrid() {
        grid.removeAllColumns();

        grid.addColumn(exam -> {
            Course course = examService.getCourseById(exam.getCourseId()).orElse(null);
            return course != null ? course.getCourseName() : "";
        }).setHeader("Course").setAutoWidth(true);

        grid.addColumn("examName").setHeader("Exam Name").setAutoWidth(true);
        grid.addColumn("examType").setHeader("Type").setAutoWidth(true);
        grid.addColumn("examDate").setHeader("Date").setAutoWidth(true);
        grid.addColumn("startTime").setHeader("Start Time").setAutoWidth(true);
        grid.addColumn("endTime").setHeader("End Time").setAutoWidth(true);
        grid.addColumn("totalMarks").setHeader("Total Marks").setAutoWidth(true);
        grid.addColumn("passingMarks").setHeader("Passing Marks").setAutoWidth(true);
        grid.addColumn("examInstructions").setHeader("Instructions").setAutoWidth(true);

        // Style the grid
        grid.setHeight("100%");
    }

    private void updateGrid() {
        if (currentUser != null) {
            // Get the list of courses the student is enrolled in
            List<Long> studentCourseIds = courseService.getStudentEnrollments(currentUser)
                    .stream()
                    .map(Enrollment::getCourse)
                    .map(Course::getCourseId)
                    .collect(Collectors.toList());

            // Filter exams to only show those for the student's courses
            List<Exam> studentExams = examService.getAllExams()
                    .stream()
                    .filter(exam -> studentCourseIds.contains(exam.getCourseId()))
                    .collect(Collectors.toList());

            grid.setItems(studentExams);
        } else {
            grid.setItems();
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.isLoggedIn()) {
            event.rerouteTo(LoginView.class);
        }
    }
}