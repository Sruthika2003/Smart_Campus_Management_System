package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "attendance/correction", layout = MainLayout.class)
@PageTitle("Attendance Correction Requests | Smart Campus")
public class StudentAttendanceCorrectionView extends VerticalLayout {

    private final SecurityService securityService;
    private final CourseService courseService;
    private final Grid<CorrectionRequest> requestsGrid = new Grid<>();
    private final ComboBox<Course> courseSelector = new ComboBox<>("Select Course");
    private final DatePicker datePicker = new DatePicker("Select Date");
    private final TextArea reasonField = new TextArea("Reason for Correction");
    
    // Sample data for demo
    private final List<CorrectionRequest> correctionRequests = new ArrayList<>();

    public StudentAttendanceCorrectionView(SecurityService securityService, CourseService courseService) {
        this.securityService = securityService;
        this.courseService = courseService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        H2 viewTitle = new H2("Attendance Correction Requests");
        add(viewTitle);
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null) {
            add(new Span("Please log in to access this page"));
            return;
        }
        
        // Create correction request form
        VerticalLayout requestForm = new VerticalLayout();
        requestForm.setPadding(true);
        requestForm.setSpacing(true);
        requestForm.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        requestForm.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        
        H4 formTitle = new H4("Submit New Correction Request");
        
        // Configure the courseSelector
        List<Course> enrolledCourses = courseService.getEnrolledCourses(currentUser);
        courseSelector.setItems(enrolledCourses);
        courseSelector.setItemLabelGenerator(Course::getCourseName);
        courseSelector.setPlaceholder("Choose a course");
        courseSelector.setWidthFull();
        
        // Configure the datePicker
        datePicker.setValue(LocalDate.now());
        datePicker.setMax(LocalDate.now());
        datePicker.setWidthFull();
        
        // Configure the reasonField
        reasonField.setPlaceholder("Explain why you need this attendance record corrected");
        reasonField.setMinHeight("100px");
        reasonField.setWidthFull();
        
        Button submitButton = new Button("Submit Request", e -> submitCorrectionRequest());
        submitButton.getStyle().set("margin-top", "var(--lumo-space-m)");
        
        requestForm.add(formTitle, courseSelector, datePicker, reasonField, submitButton);
        
        // Configure the requestsGrid
        requestsGrid.addColumn(request -> request.getCourse().getCourseName()).setHeader("Course").setAutoWidth(true);
        requestsGrid.addColumn(request -> request.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE)).setHeader("Date").setAutoWidth(true);
        requestsGrid.addColumn(CorrectionRequest::getReason).setHeader("Reason").setAutoWidth(true);
        requestsGrid.addColumn(CorrectionRequest::getStatus).setHeader("Status").setAutoWidth(true);
        
        // Initialize with sample data
        initSampleData(currentUser, enrolledCourses);
        requestsGrid.setItems(correctionRequests);
        
        add(requestForm, new H4("Your Correction Requests"), requestsGrid);
    }
    
    private void submitCorrectionRequest() {
        if (courseSelector.getValue() == null) {
            Notification.show("Please select a course");
            return;
        }
        
        if (reasonField.getValue().trim().isEmpty()) {
            Notification.show("Please provide a reason for your request");
            return;
        }
        
        // In a real app, this would save to the database
        CorrectionRequest newRequest = new CorrectionRequest(
            courseSelector.getValue(),
            datePicker.getValue(),
            reasonField.getValue(),
            "Pending"
        );
        
        correctionRequests.add(newRequest);
        requestsGrid.setItems(correctionRequests);
        
        // Reset form
        courseSelector.clear();
        datePicker.setValue(LocalDate.now());
        reasonField.clear();
        
        Notification notification = new Notification(
            "Correction request submitted successfully",
            3000,
            Notification.Position.MIDDLE
        );
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }
    
    private void initSampleData(User user, List<Course> courses) {
        if (!courses.isEmpty()) {
            // Add a sample pending request
            correctionRequests.add(new CorrectionRequest(
                courses.get(0),
                LocalDate.now().minusDays(7),
                "I was present but marked absent due to late arrival.",
                "Pending"
            ));
            
            // Add a sample approved request
            if (courses.size() > 1) {
                correctionRequests.add(new CorrectionRequest(
                    courses.get(1),
                    LocalDate.now().minusDays(14),
                    "I attended the class but was registered in the wrong section.",
                    "Approved"
                ));
            }
            
            // Add a sample rejected request
            if (courses.size() > 2) {
                correctionRequests.add(new CorrectionRequest(
                    courses.get(2),
                    LocalDate.now().minusDays(21),
                    "System error caused incorrect marking.",
                    "Rejected"
                ));
            }
        }
    }
    
    // Sample class for demo purposes
    private static class CorrectionRequest {
        private final Course course;
        private final LocalDate date;
        private final String reason;
        private String status;
        
        public CorrectionRequest(Course course, LocalDate date, String reason, String status) {
            this.course = course;
            this.date = date;
            this.reason = reason;
            this.status = status;
        }
        
        public Course getCourse() {
            return course;
        }
        
        public LocalDate getDate() {
            return date;
        }
        
        public String getReason() {
            return reason;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
} 