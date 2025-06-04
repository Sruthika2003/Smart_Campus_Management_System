package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Attendance;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.service.AttendanceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.smartcampus.examgrading.view.MainLayout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Route(value = "attendance/view", layout = MainLayout.class)
@PageTitle("View Attendance | Smart Campus")
public class StudentAttendanceView extends VerticalLayout {

    private final SecurityService securityService;
    private final CourseService courseService;
    private final AttendanceService attendanceService;
    
    public StudentAttendanceView(SecurityService securityService, CourseService courseService, 
                                 AttendanceService attendanceService) {
        this.securityService = securityService;
        this.courseService = courseService;
        this.attendanceService = attendanceService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("My Attendance");
        add(header);

        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            List<Course> enrolledCourses = courseService.getEnrolledCourses(currentUser);
            
            if (enrolledCourses.isEmpty()) {
                add(new H4("You are not enrolled in any courses"));
            } else {
                createAttendanceSummaryTable(currentUser, enrolledCourses);
            }
        } else {
            add(new H4("Please log in to view your attendance"));
        }
    }

    private void createAttendanceSummaryTable(User student, List<Course> courses) {
        // Create a single grid for the summary view
        Grid<CourseAttendanceSummary> summaryGrid = new Grid<>();
        summaryGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        summaryGrid.setHeight("auto");
        summaryGrid.setWidthFull();
        
        // Add columns to match the format in the second image
        summaryGrid.addColumn(summary -> summary.getCourse().getCourseCode())
            .setHeader("Course Code")
            .setAutoWidth(true)
            .setFlexGrow(0);
            
        summaryGrid.addColumn(summary -> summary.getCourse().getCourseName())
            .setHeader("Course Name")
            .setAutoWidth(true)
            .setFlexGrow(1);
            
        summaryGrid.addColumn(CourseAttendanceSummary::getClassesAttendedDisplay)
            .setHeader("Total Classes")
            .setAutoWidth(true)
            .setFlexGrow(0);
            
        summaryGrid.addComponentColumn(summary -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setWidthFull();
            layout.setSpacing(false);
            
            ProgressBar progressBar = new ProgressBar();
            progressBar.setMin(0);
            progressBar.setMax(100);
            progressBar.setValue(summary.getAttendancePercentage());
            progressBar.setWidth("150px");
            
            // Color based on attendance percentage
            if (summary.getAttendancePercentage() < 70) {
                progressBar.getStyle().set("--lumo-primary-color", "var(--lumo-error-color)");
            } else if (summary.getAttendancePercentage() < 80) {
                progressBar.getStyle().set("--lumo-primary-color", "var(--lumo-warning-color)");
            } else {
                progressBar.getStyle().set("--lumo-primary-color", "var(--lumo-success-color)");
            }
            
            Span percentageText = new Span(summary.getFormattedPercentage());
            percentageText.getStyle().set("margin-left", "8px");
            
            layout.add(progressBar, percentageText);
            layout.setAlignItems(Alignment.CENTER);
            
            return layout;
        })
        .setHeader("Percentage(%)")
        .setWidth("200px")
        .setFlexGrow(0);
        
        // Add view details / action column
        summaryGrid.addComponentColumn(summary -> {
            Button viewDetailsBtn = new Button("View Details", e -> viewAttendanceDetails(student, summary.getCourse()));
            viewDetailsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            return viewDetailsBtn;
        })
        .setHeader("Actions")
        .setAutoWidth(true)
        .setFlexGrow(0);
        
        // Fetch and prepare data
        List<CourseAttendanceSummary> summaries = new ArrayList<>();
        for (Course course : courses) {
            // Get real attendance data from service
            List<Attendance> attendances = attendanceService.getStudentCourseAttendance(student, course);
            BigDecimal percentage = attendanceService.calculateAttendancePercentage(student, course);
            
            // Calculate present and total classes
            long presentCount = attendances.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
                .count();
            
            summaries.add(new CourseAttendanceSummary(
                course,
                (int) presentCount,
                attendances.size(),
                percentage.doubleValue()
            ));
        }
        
        summaryGrid.setItems(summaries);
        add(summaryGrid);
        
        // Style the grid
        summaryGrid.getStyle()
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
            .set("border-radius", "var(--lumo-border-radius-m)");
        
        // Show warnings for low attendance
        for (CourseAttendanceSummary summary : summaries) {
            if (summary.getAttendancePercentage() < 75) {
                Notification notification = new Notification(
                    "Warning: Your attendance in " + summary.getCourse().getCourseName() + 
                    " is below 75%. Current attendance: " + summary.getFormattedPercentage(),
                    5000,
                    Notification.Position.TOP_CENTER
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            }
        }
    }
    
    // This method opens a dialog to view detailed attendance
    private void viewAttendanceDetails(User student, Course course) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Attendance Details: " + course.getCourseName());
        dialog.setWidth("800px");
        
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        
        // Get actual attendance records
        List<Attendance> records = attendanceService.getStudentCourseAttendance(student, course);
        
        // Create grid for detailed view
        Grid<Attendance> detailsGrid = new Grid<>();
        detailsGrid.setWidthFull();
        detailsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        
        detailsGrid.addColumn(a -> a.getAttendanceDate().toString())
            .setHeader("Date")
            .setAutoWidth(true);
            
        detailsGrid.addColumn(a -> a.getStatus().toString())
            .setHeader("Status")
            .setAutoWidth(true)
            .setClassNameGenerator(attendance -> {
                if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
                    return "status-present";
                } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                    return "status-absent";
                }
                return null;
            });
            
        detailsGrid.addComponentColumn(attendance -> {
            if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                // Check if there's already a pending correction request
                boolean hasPendingRequest = false;
                try {
                    List<com.smartcampus.examgrading.model.AttendanceCorrectionRequest> requests = 
                        attendanceService.getStudentCorrectionRequests(student);
                    hasPendingRequest = requests.stream()
                        .anyMatch(req -> req.getAttendance().getAttendanceId().equals(attendance.getAttendanceId()));
                } catch (Exception e) {
                    // Handle exception
                }
                
                if (hasPendingRequest) {
                    Span pendingSpan = new Span("Correction Requested");
                    pendingSpan.getStyle()
                        .set("color", "var(--lumo-primary-color)")
                        .set("background-color", "var(--lumo-primary-color-10pct)")
                        .set("padding", "0.25em 0.75em")
                        .set("border-radius", "var(--lumo-border-radius-s)")
                        .set("font-size", "14px");
                    return pendingSpan;
                } else {
                    Button requestButton = new Button("Request Correction");
                    requestButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
                    requestButton.addClickListener(e -> openCorrectionRequestDialog(course, attendance, dialog));
                    return requestButton;
                }
            }
            return new Span("");
        }).setHeader("Actions").setAutoWidth(true);
        
        // Custom styling for Present/Absent status
        getElement().executeJs(
            "document.head.innerHTML += '<style>" +
            ".status-present { color: var(--lumo-success-color); font-weight: 500; }" +
            ".status-absent { color: var(--lumo-error-color); font-weight: 500; }" +
            "</style>'");
        
        detailsGrid.setItems(records);
        content.add(detailsGrid);
        
        Button closeButton = new Button("Close", e -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        content.add(closeButton);
        
        dialog.add(content);
        dialog.open();
    }
    
    private void openCorrectionRequestDialog(Course course, Attendance attendance, Dialog parentDialog) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Request Attendance Correction");
        
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        
        Span info = new Span("Course: " + course.getCourseName() + ", Date: " + attendance.getAttendanceDate());
        TextArea reasonField = new TextArea("Reason for correction");
        reasonField.setWidthFull();
        reasonField.setMinHeight("100px");
        
        Button submitButton = new Button("Submit Request");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submitButton.addClickListener(e -> {
            if (reasonField.getValue().trim().isEmpty()) {
                Notification.show("Please provide a reason for the correction request");
            } else {
                try {
                    // Get the current user (student)
                    User student = securityService.getCurrentUser();
                    
                    // Create a correction request directly through the attendance service
                    // This ensures it's properly recorded for faculty to see
                    attendanceService.submitCorrectionRequest(
                        attendance, 
                        student,
                        reasonField.getValue()
                    );
                    
                    // Show notification and close dialog
                    Notification notification = Notification.show(
                        "Correction request submitted successfully to faculty"
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                    dialog.close();
                    parentDialog.close();
                    
                    // Refresh the view
                    refreshView();
                } catch (Exception ex) {
                    Notification notification = Notification.show(
                        "Error submitting correction request: " + ex.getMessage()
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        HorizontalLayout buttons = new HorizontalLayout(submitButton, cancelButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);
        
        layout.add(info, reasonField, buttons);
        dialog.add(layout);
        dialog.open();
    }
    
    private void refreshView() {
        removeAll();
        
        H2 header = new H2("My Attendance");
        add(header);
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            List<Course> enrolledCourses = courseService.getEnrolledCourses(currentUser);
            if (!enrolledCourses.isEmpty()) {
                createAttendanceSummaryTable(currentUser, enrolledCourses);
            }
        }
    }
    
    // Data model for course attendance summary
    private static class CourseAttendanceSummary {
        private final Course course;
        private final int classesAttended;
        private final int totalClasses;
        private final double attendancePercentage;
        
        public CourseAttendanceSummary(Course course, int classesAttended, int totalClasses, double attendancePercentage) {
            this.course = course;
            this.classesAttended = classesAttended;
            this.totalClasses = totalClasses;
            this.attendancePercentage = attendancePercentage;
        }
        
        public Course getCourse() {
            return course;
        }
        
        public int getClassesAttended() {
            return classesAttended;
        }
        
        public int getTotalClasses() {
            return totalClasses;
        }
        
        public String getClassesAttendedDisplay() {
            return classesAttended + "/" + totalClasses;
        }
        
        public double getAttendancePercentage() {
            return attendancePercentage;
        }
        
        public String getFormattedPercentage() {
            return String.format("%.1f%%", attendancePercentage);
        }
    }
} 