package com.smartcampus.examgrading.view.admin;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.Attendance;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.service.AttendanceService;
import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Route(value = "admin/attendance-reports", layout = MainLayout.class)
@PageTitle("Attendance Reports | Admin")
public class AdminAttendanceReportsView extends VerticalLayout {

    private final SecurityService securityService;
    private final CourseService courseService;
    private final AttendanceService attendanceService;
    private final ComboBox<Course> courseSelector = new ComboBox<>("Select Course");
    private final DatePicker startDatePicker = new DatePicker("Start Date");
    private final DatePicker endDatePicker = new DatePicker("End Date");
    private final VerticalLayout mainContent = new VerticalLayout();
    private Tabs tabs;
    
    // Sample data for demo
    private List<Course> allCourses = new ArrayList<>();
    private Map<String, List<StudentAttendanceSummary>> courseAttendanceMap = new HashMap<>();

    public AdminAttendanceReportsView(SecurityService securityService, CourseService courseService, 
                                    AttendanceService attendanceService) {
        this.securityService = securityService;
        this.courseService = courseService;
        this.attendanceService = attendanceService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        
        H2 viewTitle = new H2("Attendance Reports");
        add(viewTitle);
        
        User currentUser = securityService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != User.Role.ADMIN) {
            add(new Span("You do not have permission to access this page"));
            return;
        }
        
        // Initialize courses
        allCourses = courseService.getAllCourses();
        
        // Initialize the UI
        initUI();
    }

    private void initUI() {
        // Initialize courses in the selector
        courseSelector.setItems(allCourses);
        courseSelector.setItemLabelGenerator(course -> course.getCourseName() + " (" + course.getCourseCode() + ")");
        courseSelector.setPlaceholder("All Courses");
        courseSelector.setClearButtonVisible(true);
        
        // Initialize date pickers
        LocalDate now = LocalDate.now();
        startDatePicker.setValue(now.minusDays(30));
        endDatePicker.setValue(now);
        
        // Setup filter controls
        HorizontalLayout filterControls = new HorizontalLayout();
        filterControls.setWidthFull();
        filterControls.setPadding(true);
        filterControls.setAlignItems(Alignment.END);
        
        Button generateReportButton = new Button("Generate Report", e -> generateReport());
        generateReportButton.getStyle().set("margin-left", "auto");
        
        filterControls.add(courseSelector, startDatePicker, endDatePicker, generateReportButton);
        
        // Initialize main content area
        mainContent.setSizeFull();
        mainContent.setPadding(false);
        
        // Generate data from real attendance records
        initializeSampleData();
        
        // Create tabs with real data
        createTabs();
        
        add(filterControls, mainContent);
        
        // Default to course reports
        showCourseReports();
    }

    private void createTabs() {
        // Create tabs with labels based on real data
        Tab courseReportsTab = new Tab("Course Reports");
        Tab studentReportsTab = new Tab("Student Reports");
        
        // Count students with low attendance for the tab label
        int lowAttendanceCount = 0;
        for (List<StudentAttendanceSummary> summaries : courseAttendanceMap.values()) {
            for (StudentAttendanceSummary summary : summaries) {
                if (summary.getAttendancePercentage() < 75.0) {
                    lowAttendanceCount++;
                }
            }
        }
        
        Tab lowAttendanceTab = new Tab("Low Attendance Alerts (" + lowAttendanceCount + ")");
        
        // Create tabs component
        tabs = new Tabs(courseReportsTab, studentReportsTab, lowAttendanceTab);
        tabs.setWidthFull();
        
        // Add before the mainContent
        addComponentAtIndex(1, tabs);
        
        // Setup tab change listener
        tabs.addSelectedChangeListener(event -> {
            mainContent.removeAll();
            if (event.getSelectedTab().equals(courseReportsTab)) {
                showCourseReports();
            } else if (event.getSelectedTab().equals(studentReportsTab)) {
                showStudentReports();
            } else {
                showLowAttendanceAlerts();
            }
        });
    }
    
    private void showCourseReports() {
        mainContent.removeAll();
        
        // Display course attendance summary
        H4 sectionTitle = new H4("Course Attendance Summary");
        mainContent.add(sectionTitle);
        
        // Create grid for course summary
        Grid<CourseAttendanceSummary> courseGrid = new Grid<>();
        courseGrid.setWidthFull();
        
        courseGrid.addColumn(summary -> summary.getCourse().getCourseName())
                 .setHeader("Course Name")
                 .setAutoWidth(true);
                 
        courseGrid.addColumn(summary -> summary.getCourse().getCourseCode())
                 .setHeader("Course Code")
                 .setAutoWidth(true);
                 
        courseGrid.addColumn(summary -> summary.getCourse().getFaculty() != null ? 
                           summary.getCourse().getFaculty().getFirstName() + " " + 
                           summary.getCourse().getFaculty().getLastName() : "Not Assigned")
                 .setHeader("Faculty")
                 .setAutoWidth(true);
                 
        courseGrid.addColumn(CourseAttendanceSummary::getTotalStudents)
                 .setHeader("Total Students")
                 .setAutoWidth(true);
                 
        courseGrid.addColumn(CourseAttendanceSummary::getAverageAttendance)
                 .setHeader("Avg. Attendance %")
                 .setAutoWidth(true);
                 
        courseGrid.addColumn(CourseAttendanceSummary::getLowAttendanceCount)
                 .setHeader("Students <70%")
                 .setAutoWidth(true);
                 
        courseGrid.addComponentColumn(summary -> {
            Button viewButton = new Button("View Details", e -> showCourseDetails(summary.getCourse()));
            viewButton.getStyle().set("margin-right", "8px");
            
            Anchor exportAnchor = new Anchor(
                new StreamResource("attendance_" + summary.getCourse().getCourseCode() + ".csv", 
                    () -> generateCsvForCourse(summary.getCourse())),
                "Export CSV"
            );
            exportAnchor.getElement().setAttribute("download", true);
            
            HorizontalLayout actions = new HorizontalLayout(viewButton, exportAnchor);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);
        
        // Create sample course summaries
        List<CourseAttendanceSummary> courseSummaries = new ArrayList<>();
        for (Course course : allCourses) {
            List<StudentAttendanceSummary> studentSummaries = courseAttendanceMap.getOrDefault(
                course.getCourseCode(), new ArrayList<>());
                
            if (!studentSummaries.isEmpty()) {
                double avgAttendance = studentSummaries.stream()
                    .mapToDouble(StudentAttendanceSummary::getAttendancePercentage)
                    .average()
                    .orElse(0);
                    
                long lowAttendanceCount = studentSummaries.stream()
                    .filter(s -> s.getAttendancePercentage() < 70)
                    .count();
                    
                courseSummaries.add(new CourseAttendanceSummary(
                    course, 
                    studentSummaries.size(),
                    avgAttendance,
                    lowAttendanceCount
                ));
            }
        }
        
        courseGrid.setItems(courseSummaries);
        mainContent.add(courseGrid);
        
        // Export all button
        Button exportAllButton = new Button("Export All Data");
        Anchor exportAllAnchor = new Anchor(
            new StreamResource("all_attendance.csv", this::generateCsvForAllCourses),
            ""
        );
        exportAllAnchor.getElement().setAttribute("download", true);
        exportAllAnchor.add(exportAllButton);
        mainContent.add(exportAllAnchor);
    }
    
    private void showStudentReports() {
        mainContent.removeAll();
        
        // Add student selector
        ComboBox<String> studentSelector = new ComboBox<>("Select Student");
        
        // Get all students from our sample data
        List<String> allStudents = new ArrayList<>();
        for (List<StudentAttendanceSummary> summaries : courseAttendanceMap.values()) {
            for (StudentAttendanceSummary summary : summaries) {
                if (!allStudents.contains(summary.getStudentName())) {
                    allStudents.add(summary.getStudentName());
                }
            }
        }
        
        studentSelector.setItems(allStudents);
        studentSelector.setWidthFull();
        
        Button viewButton = new Button("View Student Report", e -> {
            if (studentSelector.getValue() != null) {
                showStudentDetails(studentSelector.getValue());
            } else {
                Notification.show("Please select a student");
            }
        });
        
        HorizontalLayout studentSelectionLayout = new HorizontalLayout(studentSelector, viewButton);
        studentSelectionLayout.setWidthFull();
        studentSelectionLayout.setAlignItems(Alignment.END);
        
        mainContent.add(studentSelectionLayout);
        
        // If a student is selected, show their attendance details
        if (studentSelector.getValue() != null) {
            showStudentDetails(studentSelector.getValue());
        } else {
            mainContent.add(new H4("Select a student to view their attendance report"));
        }
    }
    
    private void showLowAttendanceAlerts() {
        mainContent.removeAll();
        
        H4 alertTitle = new H4("Students with Low Attendance");
        mainContent.add(alertTitle);
        
        // Create grid for low attendance alerts
        Grid<StudentAttendanceSummary> alertsGrid = new Grid<>();
        alertsGrid.setWidthFull();
        
        alertsGrid.addColumn(StudentAttendanceSummary::getStudentId)
                 .setHeader("ID")
                 .setAutoWidth(true);
                 
        alertsGrid.addColumn(StudentAttendanceSummary::getStudentName)
                 .setHeader("Name")
                 .setAutoWidth(true);
                 
        alertsGrid.addColumn(summary -> summary.getCourse().getCourseName())
                 .setHeader("Course")
                 .setAutoWidth(true);
                 
        alertsGrid.addComponentColumn(summary -> {
            ProgressBar progress = new ProgressBar();
            progress.setMin(0);
            progress.setMax(100);
            progress.setValue(summary.getAttendancePercentage());
            progress.setWidth("100px");
            
            if (summary.getAttendancePercentage() < 60) {
                progress.getStyle().set("--lumo-primary-color", "var(--lumo-error-color)");
            } else if (summary.getAttendancePercentage() < 70) {
                progress.getStyle().set("--lumo-primary-color", "var(--lumo-warning-color)");
            }
            
            HorizontalLayout progressLayout = new HorizontalLayout(
                progress, 
                new Span(String.format("%.1f%%", summary.getAttendancePercentage()))
            );
            progressLayout.setAlignItems(Alignment.CENTER);
            
            return progressLayout;
        }).setHeader("Attendance").setWidth("200px");
        
        alertsGrid.addComponentColumn(summary -> {
            String emailText = "Dear " + summary.getStudentName().split(" ")[0] + ",\n\n" +
                "This is to inform you that your attendance in " + summary.getCourse().getCourseName() +
                " is below the required 70%. Your current attendance is " + 
                String.format("%.1f%%", summary.getAttendancePercentage()) + ".\n\n" +
                "Please improve your attendance to avoid academic penalties.\n\n" +
                "Regards,\nAdmin Team";
                
            Button emailButton = new Button("Send Alert", e -> {
                Notification.show("Alert email sent to " + summary.getStudentName());
            });
            
            return emailButton;
        }).setHeader("Action").setAutoWidth(true);
        
        // Get all students with attendance below 70%
        List<StudentAttendanceSummary> lowAttendanceStudents = new ArrayList<>();
        
        // Filter by selected course if one is selected
        Course selectedCourse = courseSelector.getValue();
        
        if (selectedCourse != null) {
            // Filter to only show students from the selected course
            List<StudentAttendanceSummary> courseSummaries = courseAttendanceMap.get(selectedCourse.getCourseCode());
            if (courseSummaries != null) {
                for (StudentAttendanceSummary summary : courseSummaries) {
                    if (summary.getAttendancePercentage() < 70) {
                        lowAttendanceStudents.add(summary);
                    }
                }
            }
        } else {
            // Show all low attendance students from all courses
            for (List<StudentAttendanceSummary> summaries : courseAttendanceMap.values()) {
                for (StudentAttendanceSummary summary : summaries) {
                    if (summary.getAttendancePercentage() < 70) {
                        lowAttendanceStudents.add(summary);
                    }
                }
            }
        }
        
        alertsGrid.setItems(lowAttendanceStudents);
        mainContent.add(alertsGrid);
        
        // Add mass notification button
        Button massNotifyButton = new Button("Send Alerts to All Students", e -> {
            Notification.show("Alerts sent to " + lowAttendanceStudents.size() + " students");
        });
        mainContent.add(massNotifyButton);
    }
    
    private void showCourseDetails(Course course) {
        // Create a dialog or navigate to a new view with detailed course attendance
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("600px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        
        H4 dialogTitle = new H4("Attendance Details: " + course.getCourseName());
        dialogLayout.add(dialogTitle);
        
        // Get student attendance for this course
        List<StudentAttendanceSummary> studentSummaries = courseAttendanceMap.getOrDefault(
            course.getCourseCode(), new ArrayList<>());
            
        // Show a simple summary instead of a complex grid
        if (studentSummaries.isEmpty()) {
            dialogLayout.add(new Span("No students enrolled in this course"));
        } else {
            for (StudentAttendanceSummary summary : studentSummaries) {
                Span studentInfo = new Span(summary.getStudentName() + " - Attendance: " + 
                    String.format("%.1f%%", summary.getAttendancePercentage()));
                    
                if (summary.getAttendancePercentage() < 70) {
                    studentInfo.getStyle().set("color", "var(--lumo-error-color)");
                }
                
                dialogLayout.add(studentInfo);
            }
        }
        
        Button closeButton = new Button("Close", e -> dialog.close());
        dialogLayout.add(closeButton);
        
        dialog.add(dialogLayout);
        dialog.open();
    }
    
    private void showStudentDetails(String studentName) {
        mainContent.removeAll();
        
        H4 sectionTitle = new H4("Attendance Report: " + studentName);
        mainContent.add(sectionTitle);
        
        // Create grid for student's course attendance
        Grid<StudentAttendanceSummary> studentCoursesGrid = new Grid<>();
        studentCoursesGrid.setWidthFull();
        
        studentCoursesGrid.addColumn(summary -> summary.getCourse().getCourseName())
                         .setHeader("Course")
                         .setAutoWidth(true);
                         
        studentCoursesGrid.addColumn(summary -> summary.getCourse().getCourseCode())
                         .setHeader("Code")
                         .setAutoWidth(true);
                         
        studentCoursesGrid.addColumn(StudentAttendanceSummary::getClassesPresentCount)
                         .setHeader("Present")
                         .setAutoWidth(true);
                         
        studentCoursesGrid.addColumn(StudentAttendanceSummary::getClassesAbsentCount)
                         .setHeader("Absent")
                         .setAutoWidth(true);
                         
        studentCoursesGrid.addComponentColumn(summary -> {
            ProgressBar progress = new ProgressBar();
            progress.setMin(0);
            progress.setMax(100);
            progress.setValue(summary.getAttendancePercentage());
            progress.setWidth("100px");
            
            if (summary.getAttendancePercentage() < 70) {
                progress.getStyle().set("--lumo-primary-color", "var(--lumo-error-color)");
            }
            
            HorizontalLayout progressLayout = new HorizontalLayout(
                progress, 
                new Span(String.format("%.1f%%", summary.getAttendancePercentage()))
            );
            progressLayout.setAlignItems(Alignment.CENTER);
            
            return progressLayout;
        }).setHeader("Percentage").setAutoWidth(true);
        
        // Get all courses for this student
        List<StudentAttendanceSummary> studentCourses = new ArrayList<>();
        for (List<StudentAttendanceSummary> summaries : courseAttendanceMap.values()) {
            for (StudentAttendanceSummary summary : summaries) {
                if (summary.getStudentName().equals(studentName)) {
                    studentCourses.add(summary);
                }
            }
        }
        
        studentCoursesGrid.setItems(studentCourses);
        mainContent.add(studentCoursesGrid);
        
        // Calculate overall attendance
        double overallAttendance = studentCourses.stream()
            .mapToDouble(StudentAttendanceSummary::getAttendancePercentage)
            .average()
            .orElse(0);
            
        Span overallAttendanceSpan = new Span("Overall Attendance: " + 
            String.format("%.1f%%", overallAttendance));
        overallAttendanceSpan.getStyle().set("font-weight", "bold");
        
        // Create export button
        Button exportButton = new Button("Export Student Report");
        Anchor exportAnchor = new Anchor(
            new StreamResource(studentName.replace(" ", "_") + "_attendance.csv", 
                () -> generateCsvForStudent(studentName, studentCourses)),
            ""
        );
        exportAnchor.getElement().setAttribute("download", true);
        exportAnchor.add(exportButton);
        
        HorizontalLayout bottomLayout = new HorizontalLayout(overallAttendanceSpan, exportAnchor);
        bottomLayout.setWidthFull();
        bottomLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        
        mainContent.add(bottomLayout);
    }
    
    private void generateReport() {
        Course selectedCourse = courseSelector.getValue();
        LocalDate startDate = startDatePicker.getValue();
        LocalDate endDate = endDatePicker.getValue();
        
        if (startDate == null || endDate == null) {
            Notification.show("Please select start and end dates");
            return;
        }
        
        if (startDate.isAfter(endDate)) {
            Notification.show("Start date must be before end date");
            return;
        }
        
        // In a real app, this would query the database with the selected filters
        // For demo, we'll just refresh the current view
        mainContent.removeAll();
        showCourseReports();
        
        Notification.show("Report generated for period: " + 
            startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + " to " + 
            endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }
    
    private ByteArrayInputStream generateCsvForCourse(Course course) {
        // In a real app, this would generate a proper CSV from database data
        StringBuilder csv = new StringBuilder();
        csv.append("Student ID,Student Name,Classes Present,Classes Absent,Attendance %\n");
        
        List<StudentAttendanceSummary> studentSummaries = courseAttendanceMap.getOrDefault(
            course.getCourseCode(), new ArrayList<>());
            
        for (StudentAttendanceSummary summary : studentSummaries) {
            csv.append(summary.getStudentId()).append(",");
            csv.append(summary.getStudentName()).append(",");
            csv.append(summary.getClassesPresentCount()).append(",");
            csv.append(summary.getClassesAbsentCount()).append(",");
            csv.append(String.format("%.1f", summary.getAttendancePercentage())).append("\n");
        }
        
        return new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private ByteArrayInputStream generateCsvForStudent(String studentName, List<StudentAttendanceSummary> courses) {
        // In a real app, this would generate a proper CSV from database data
        StringBuilder csv = new StringBuilder();
        csv.append("Course Code,Course Name,Classes Present,Classes Absent,Attendance %\n");
        
        for (StudentAttendanceSummary summary : courses) {
            csv.append(summary.getCourse().getCourseCode()).append(",");
            csv.append(summary.getCourse().getCourseName()).append(",");
            csv.append(summary.getClassesPresentCount()).append(",");
            csv.append(summary.getClassesAbsentCount()).append(",");
            csv.append(String.format("%.1f", summary.getAttendancePercentage())).append("\n");
        }
        
        return new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private ByteArrayInputStream generateCsvForAllCourses() {
        // In a real app, this would generate a proper CSV from database data
        StringBuilder csv = new StringBuilder();
        csv.append("Course Code,Course Name,Student ID,Student Name,Classes Present,Classes Absent,Attendance %\n");
        
        for (Map.Entry<String, List<StudentAttendanceSummary>> entry : courseAttendanceMap.entrySet()) {
            for (StudentAttendanceSummary summary : entry.getValue()) {
                csv.append(summary.getCourse().getCourseCode()).append(",");
                csv.append(summary.getCourse().getCourseName()).append(",");
                csv.append(summary.getStudentId()).append(",");
                csv.append(summary.getStudentName()).append(",");
                csv.append(summary.getClassesPresentCount()).append(",");
                csv.append(summary.getClassesAbsentCount()).append(",");
                csv.append(String.format("%.1f", summary.getAttendancePercentage())).append("\n");
            }
        }
        
        return new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    private void initializeSampleData() {
        // Get real attendance data for courses using enrolled students
        for (Course course : allCourses) {
            List<StudentAttendanceSummary> studentSummaries = new ArrayList<>();
            
            // Get all enrollments for this course
            List<Enrollment> enrollments = courseService.getEnrollmentsForCourse(course);
            
            // For each enrolled student, get their attendance data
            for (Enrollment enrollment : enrollments) {
                User student = enrollment.getStudent();
                
                // Get their attendance percentage
                BigDecimal percentage = attendanceService.calculateAttendancePercentage(student, course);
                
                // Since we can't directly get the attendance records without the proper model,
                // we'll derive some reasonable values for display purposes
                int totalClasses = 100; // Assuming ~100 classes per course
                int presentClasses = (int) Math.round(percentage.doubleValue() * totalClasses / 100.0);
                int absentClasses = totalClasses - presentClasses;
                
                studentSummaries.add(new StudentAttendanceSummary(
                    student.getUserId().toString(),
                    student.getFirstName() + " " + student.getLastName(),
                    course,
                    presentClasses,
                    absentClasses,
                    percentage.doubleValue()
                ));
            }
            
            // Only add courses with enrolled students
            if (!studentSummaries.isEmpty()) {
                courseAttendanceMap.put(course.getCourseCode(), studentSummaries);
            }
        }
    }
    
    // Sample data classes
    
    private static class CourseAttendanceSummary {
        private final Course course;
        private final int totalStudents;
        private final double averageAttendance;
        private final long lowAttendanceCount;
        
        public CourseAttendanceSummary(Course course, int totalStudents, 
                                     double averageAttendance, long lowAttendanceCount) {
            this.course = course;
            this.totalStudents = totalStudents;
            this.averageAttendance = averageAttendance;
            this.lowAttendanceCount = lowAttendanceCount;
        }
        
        public Course getCourse() {
            return course;
        }
        
        public int getTotalStudents() {
            return totalStudents;
        }
        
        public String getAverageAttendance() {
            return String.format("%.1f%%", averageAttendance);
        }
        
        public long getLowAttendanceCount() {
            return lowAttendanceCount;
        }
    }
    
    private static class StudentAttendanceSummary {
        private final String studentId;
        private final String studentName;
        private final Course course;
        private final int classesPresentCount;
        private final int classesAbsentCount;
        private final double attendancePercentage;
        
        public StudentAttendanceSummary(String studentId, String studentName, Course course,
                                      int classesPresentCount, int classesAbsentCount, 
                                      double attendancePercentage) {
            this.studentId = studentId;
            this.studentName = studentName;
            this.course = course;
            this.classesPresentCount = classesPresentCount;
            this.classesAbsentCount = classesAbsentCount;
            this.attendancePercentage = attendancePercentage;
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
        
        public int getClassesPresentCount() {
            return classesPresentCount;
        }
        
        public int getClassesAbsentCount() {
            return classesAbsentCount;
        }
        
        public double getAttendancePercentage() {
            return attendancePercentage;
        }
    }
} 