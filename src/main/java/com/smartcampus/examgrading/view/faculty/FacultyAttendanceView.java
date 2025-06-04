package com.smartcampus.examgrading.view.faculty;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.Attendance;
import com.smartcampus.examgrading.model.AttendanceCorrectionRequest;
import com.smartcampus.examgrading.repository.UserRepository;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.service.AttendanceService;
import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "faculty/attendance", layout = MainLayout.class)
@PageTitle("Attendance Management | Smart Campus")
public class FacultyAttendanceView extends VerticalLayout {

    private final SecurityService securityService;
    private final CourseService courseService;
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private ComboBox<Course> courseSelector;
    private DatePicker datePicker;
    private Grid<StudentAttendanceEntry> studentGrid;
    private Grid<CorrectionRequest> requestsGrid;
    private VerticalLayout mainContent;
    
    // For demo purposes - in a real app these would be in a database
    private List<Course> facultyCourses;
    private Map<String, List<StudentAttendanceEntry>> courseStudentsMap = new HashMap<>();
    private List<CorrectionRequest> correctionRequests = new ArrayList<>();

    public FacultyAttendanceView(SecurityService securityService, CourseService courseService,
                                AttendanceService attendanceService, UserRepository userRepository) {
        this.securityService = securityService;
        this.courseService = courseService;
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        H2 viewTitle = new H2("Attendance Management");
        add(viewTitle);
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.Role.FACULTY) {
            facultyCourses = courseService.getCoursesByFaculty(currentUser);
            
            if (facultyCourses.isEmpty()) {
                add(new H4("You do not have any courses assigned"));
            } else {
                // Initialize sample data for demo
                initializeSampleData();
                
                // Create tabbed interface
                createTabbedLayout();
            }
        } else {
            add(new Span("You do not have permission to access this page"));
        }
    }

    private void initializeSampleData() {
        // Create student attendance records from enrolled students
        for (Course course : facultyCourses) {
            List<StudentAttendanceEntry> studentEntries = new ArrayList<>();
            
            // Get all enrollments for this course to find enrolled students
            List<Enrollment> enrollments = courseService.getEnrollmentsForCourse(course);
            
            // Create an attendance entry for each enrolled student
            for (Enrollment enrollment : enrollments) {
                User student = enrollment.getStudent();
                studentEntries.add(new StudentAttendanceEntry(
                    student.getUserId().toString(),
                    student.getFirstName() + " " + student.getLastName(),
                    "Present" // Default status
                ));
            }
            
            // If no students are enrolled, add a message
            if (studentEntries.isEmpty()) {
                // We won't show anything in the grid if no students
                Notification.show("No students enrolled in " + course.getCourseName());
            }
            
            courseStudentsMap.put(course.getCourseCode(), studentEntries);
        }
        
        // Create some sample correction requests (in a real app this would come from database)
        // Only create requests for actual enrolled students
        for (Course course : facultyCourses) {
            List<Enrollment> enrollments = courseService.getEnrollmentsForCourse(course);
            
            if (!enrollments.isEmpty()) {
                // Take the first student for the sample request
                User student = enrollments.get(0).getStudent();
                
                correctionRequests.add(new CorrectionRequest(
                    student.getUserId().toString(),
                    student.getFirstName() + " " + student.getLastName(),
                    course, 
                    "2024-04-15",
                    "Absent", 
                    "I was present but marked absent. The TA can confirm I was there."
                ));
                
                // Add another request if more than one student is enrolled
                if (enrollments.size() > 1) {
                    User student2 = enrollments.get(1).getStudent();
                    correctionRequests.add(new CorrectionRequest(
                        student2.getUserId().toString(),
                        student2.getFirstName() + " " + student2.getLastName(),
                        course, 
                        "2024-04-10",
                        "Absent", 
                        "I attended the class but was late by 5 minutes due to bus delay."
                    ));
                }
                
                // Only create sample requests for the first course with enrolled students
                break;
            }
        }
    }

    private void createTabbedLayout() {
        Tab markAttendanceTab = new Tab("Mark Attendance");
        Tab correctionRequestsTab = new Tab("Correction Requests");
        
        Tabs tabs = new Tabs(markAttendanceTab, correctionRequestsTab);
        tabs.setWidthFull();
        
        mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(false);
        mainContent.setSpacing(true);
        
        add(tabs, mainContent);
        
        // Default to the first tab
        showMarkAttendanceContent();
        
        tabs.addSelectedChangeListener(event -> {
            mainContent.removeAll();
            if (event.getSelectedTab().equals(markAttendanceTab)) {
                showMarkAttendanceContent();
            } else {
                showCorrectionRequestsContent();
            }
        });
    }

    private void showMarkAttendanceContent() {
        mainContent.removeAll();
        
        // Course and date selector
        HorizontalLayout selectors = new HorizontalLayout();
        selectors.setWidthFull();
        
        courseSelector = new ComboBox<>("Select Course");
        courseSelector.setItems(facultyCourses);
        courseSelector.setItemLabelGenerator(Course::getCourseName);
        
        datePicker = new DatePicker("Select Date");
        datePicker.setValue(LocalDate.now());
        
        Button loadButton = new Button("Load Students", e -> loadStudentAttendance());
        loadButton.getStyle().set("margin-top", "32px"); // Align with the fields
        
        selectors.add(courseSelector, datePicker, loadButton);
        
        // Student attendance grid
        studentGrid = new Grid<>();
        studentGrid.addColumn(StudentAttendanceEntry::getStudentId).setHeader("Student ID").setAutoWidth(true);
        studentGrid.addColumn(StudentAttendanceEntry::getStudentName).setHeader("Name").setAutoWidth(true);
        studentGrid.addComponentColumn(entry -> {
            ComboBox<String> statusCombo = new ComboBox<>("", "Present", "Absent");
            statusCombo.setValue(entry.getStatus());
            statusCombo.addValueChangeListener(event -> entry.setStatus(event.getValue()));
            return statusCombo;
        }).setHeader("Status").setAutoWidth(true);
        
        Button saveButton = new Button("Save Attendance", e -> saveAttendance());
        saveButton.setWidthFull();
        
        mainContent.add(selectors, studentGrid, saveButton);
        studentGrid.setVisible(false);
        saveButton.setVisible(false);
    }

    private void showCorrectionRequestsContent() {
        mainContent.removeAll();
        
        // Get the current faculty user
        User faculty = securityService.getCurrentUser();
        
        try {
            // Get all pending correction requests for this faculty
            List<AttendanceCorrectionRequest> pendingRequests = 
                attendanceService.getPendingRequestsForFaculty(faculty);

            // Add a refresh button
            Button refreshButton = new Button("Refresh", e -> showCorrectionRequestsContent());
            refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            mainContent.add(refreshButton);
            
            if (pendingRequests.isEmpty()) {
                mainContent.add(new H3("No correction requests pending"));
                return;
            }
            
            // Create grid for correction requests
            Grid<AttendanceCorrectionRequest> requestsGrid = new Grid<>();
            requestsGrid.setWidthFull();
            requestsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
            
            // Add columns with proper data binding
            requestsGrid.addColumn(request -> request.getRequestedBy().getUserId().toString())
                       .setHeader("Student ID")
                       .setAutoWidth(true);
                       
            requestsGrid.addColumn(request -> 
                       request.getRequestedBy().getFirstName() + " " + 
                       request.getRequestedBy().getLastName())
                       .setHeader("Name")
                       .setAutoWidth(true);
                       
            requestsGrid.addColumn(request -> request.getAttendance().getCourse().getCourseName())
                       .setHeader("Course")
                       .setAutoWidth(true);
                       
            requestsGrid.addColumn(request -> request.getAttendance().getAttendanceDate().toString())
                       .setHeader("Date")
                       .setAutoWidth(true);
                       
            requestsGrid.addColumn(request -> request.getAttendance().getStatus().toString())
                       .setHeader("Current Status")
                       .setAutoWidth(true);
                       
            requestsGrid.addColumn(AttendanceCorrectionRequest::getReason)
                       .setHeader("Reason")
                       .setAutoWidth(true);
                       
            requestsGrid.addComponentColumn(request -> {
                HorizontalLayout actions = new HorizontalLayout();
                
                Button approveButton = new Button("Approve", e -> 
                    handleCorrectionResponse(request, true));
                approveButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                
                Button rejectButton = new Button("Reject", e -> 
                    handleCorrectionResponse(request, false));
                rejectButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
                
                actions.add(approveButton, rejectButton);
                return actions;
            }).setHeader("Actions").setAutoWidth(true);
            
            requestsGrid.setItems(pendingRequests);
            
            mainContent.add(new H3("Pending Correction Requests (" + pendingRequests.size() + ")"), requestsGrid);
        } catch (Exception e) {
            Notification notification = Notification.show(
                "Error loading correction requests: " + e.getMessage(),
                5000,
                Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            
            // Add error message and stack trace to UI for debugging
            mainContent.add(new H3("Error loading correction requests"));
            TextArea errorText = new TextArea();
            errorText.setValue(e.toString());
            errorText.setWidthFull();
            errorText.setReadOnly(true);
            mainContent.add(errorText);
        }
    }

    private void loadStudentAttendance() {
        if (courseSelector.getValue() == null) {
            Notification.show("Please select a course");
            return;
        }
        
        String courseCode = courseSelector.getValue().getCourseCode();
        List<StudentAttendanceEntry> students = courseStudentsMap.get(courseCode);
        
        if (students.isEmpty()) {
            Notification.show("No students enrolled in this course");
            return;
        }
        
        studentGrid.setItems(students);
        studentGrid.setVisible(true);
        mainContent.getChildren().filter(component -> component instanceof Button)
            .findFirst().get().setVisible(true);
    }

    private void saveAttendance() {
        if (courseSelector.getValue() == null || datePicker.getValue() == null) {
            Notification.show("Please select a course and date");
            return;
        }
        
        Course selectedCourse = courseSelector.getValue();
        LocalDate selectedDate = datePicker.getValue();
        User faculty = securityService.getCurrentUser();
        
        List<User> students = new ArrayList<>();
        List<Attendance.AttendanceStatus> statuses = new ArrayList<>();
        
        // Collect all students and their attendance statuses
        for (StudentAttendanceEntry entry : studentGrid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList())) {
            // Find actual student in the system by ID
            try {
                Long studentId = Long.parseLong(entry.getStudentId());
                User student = userRepository.findById(studentId).orElse(null);
                
                if (student != null) {
                    students.add(student);
                    
                    // Convert status string to enum
                    Attendance.AttendanceStatus status = "Present".equals(entry.getStatus()) 
                        ? Attendance.AttendanceStatus.PRESENT 
                        : Attendance.AttendanceStatus.ABSENT;
                        
                    statuses.add(status);
                }
            } catch (NumberFormatException e) {
                // Skip invalid student IDs
                continue;
            }
        }
        
        if (!students.isEmpty()) {
            try {
                // Save to database through service
                attendanceService.markBulkAttendance(selectedCourse, students, selectedDate, statuses, faculty);
        Notification.show("Attendance saved successfully");
            } catch (Exception e) {
                Notification.show("Error saving attendance: " + e.getMessage());
            }
        } else {
            Notification.show("No valid students found to mark attendance");
        }
    }

    private void handleCorrectionResponse(AttendanceCorrectionRequest request, boolean approved) {
        Dialog responseDialog = new Dialog();
        responseDialog.setHeaderTitle(approved ? "Approve Correction Request" : "Reject Correction Request");
        
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        
        TextArea commentField = new TextArea("Comments (optional)");
        commentField.setWidthFull();
        
        Button confirmButton = new Button("Confirm", e -> {
            try {
                // Process the request through service
                User faculty = securityService.getCurrentUser();
                AttendanceCorrectionRequest.RequestStatus status = approved ? 
                    AttendanceCorrectionRequest.RequestStatus.APPROVED : 
                    AttendanceCorrectionRequest.RequestStatus.REJECTED;
                
                attendanceService.reviewCorrectionRequest(
                    request.getRequestId(), 
                    status, 
                    faculty, 
                    commentField.getValue()
                );
                
                Notification notification = Notification.show(
                    "Request " + (approved ? "approved" : "rejected") + " successfully"
                );
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                responseDialog.close();
                showCorrectionRequestsContent(); // Refresh the view
            } catch (Exception ex) {
                Notification notification = Notification.show(
                    "Error processing request: " + ex.getMessage()
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmButton.addThemeVariants(
            approved ? ButtonVariant.LUMO_SUCCESS : ButtonVariant.LUMO_ERROR
        );
        
        Button cancelButton = new Button("Cancel", e -> responseDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(JustifyContentMode.END);
        
        dialogLayout.add(commentField, buttons);
        responseDialog.add(dialogLayout);
        responseDialog.open();
    }

    // Sample classes for demo purposes
    
    private static class StudentAttendanceEntry {
        private final String studentId;
        private final String studentName;
        private String status;
        
        public StudentAttendanceEntry(String studentId, String studentName, String status) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.status = status;
        }
        
        public String getStudentId() {
            return studentId;
        }
        
        public String getStudentName() {
            return studentName;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
    }
    
    private static class CorrectionRequest {
        private final String studentId;
        private final String studentName;
        private final Course course;
        private final String date;
        private final String currentStatus;
        private final String reason;
        
        public CorrectionRequest(String studentId, String studentName, Course course, 
                               String date, String currentStatus, String reason) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.course = course;
            this.date = date;
            this.currentStatus = currentStatus;
            this.reason = reason;
        }
        
        public String getStudentId() {
            return studentId;
        }
        
        public String getStudentName() {
            return studentName;
        }
        
        public Course getCourse() {
            return course;
        }
        
        public String getDate() {
            return date;
        }
        
        public String getCurrentStatus() {
            return currentStatus;
        }
        
        public String getReason() {
            return reason;
        }
    }
} 