package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.service.AttendanceService;
import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Route(value = "student/dashboard", layout = MainLayout.class)
@PageTitle("Student Dashboard | Smart Campus")
public class StudentDashboardView extends VerticalLayout {

    private final SecurityService securityService;
    private final CourseService courseService;
    private final AttendanceService attendanceService;

    public StudentDashboardView(SecurityService securityService, CourseService courseService, AttendanceService attendanceService) {
        this.securityService = securityService;
        this.courseService = courseService;
        this.attendanceService = attendanceService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        User currentUser = securityService.getCurrentUser();
        if (currentUser != null) {
            add(new H2("Welcome back, " + currentUser.getFirstName() + "!"));
            
            createDashboard(currentUser);
        } else {
            add(new H4("Please log in to view your dashboard"));
        }
    }

    private void createDashboard(User student) {
        List<Course> enrolledCourses = courseService.getEnrolledCourses(student);
        
        // Create dashboard cards in a horizontal layout
        HorizontalLayout statCards = new HorizontalLayout();
        statCards.setWidthFull();
        statCards.setSpacing(true);
        statCards.add(
            createStatCard("Courses", String.valueOf(enrolledCourses.size()), VaadinIcon.ACADEMY_CAP, "var(--lumo-primary-color)"),
            createAttendanceCard(enrolledCourses),
            createExamCard(enrolledCourses),
            createFeesCard()
        );
        
        add(statCards);
        
        // Add quick links section
        VerticalLayout quickLinksSection = new VerticalLayout();
        quickLinksSection.setPadding(false);
        quickLinksSection.setSpacing(true);
        
        H4 quickLinksTitle = new H4("Quick Links");
        quickLinksSection.add(quickLinksTitle);
        
        HorizontalLayout quickLinks = new HorizontalLayout();
        quickLinks.setWidthFull();
        quickLinks.setSpacing(true);
        
        quickLinks.add(
            createQuickLink("View Attendance", VaadinIcon.CLOCK, "attendance/view"),
            createQuickLink("Request Correction", VaadinIcon.ENVELOPE, "attendance/correction"),
            createQuickLink("Attendance Alerts", VaadinIcon.BELL, "attendance/alerts"),
            createQuickLink("Fee Management", VaadinIcon.MONEY, "fees"),
            createQuickLink("Exam Schedule", VaadinIcon.CALENDAR_CLOCK, "exams")
        );
        
        quickLinksSection.add(quickLinks);
        add(quickLinksSection);
        
        // Recent activity section
        if (!enrolledCourses.isEmpty()) {
            add(new H4("Your Recent Courses"));
            
            VerticalLayout recentCourses = new VerticalLayout();
            recentCourses.setPadding(false);
            recentCourses.setSpacing(true);
            
            for (int i = 0; i < Math.min(3, enrolledCourses.size()); i++) {
                Course course = enrolledCourses.get(i);
                
                Div courseCard = new Div();
                courseCard.addClassName("course-card");
                courseCard.getStyle()
                    .set("border", "1px solid var(--lumo-contrast-20pct)")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("padding", "var(--lumo-space-m)")
                    .set("margin-bottom", "var(--lumo-space-m)");
                
                H4 courseName = new H4(course.getCourseName() + " (" + course.getCourseCode() + ")");
                Span faculty = new Span("Faculty: " + (course.getFaculty() != null ? 
                    course.getFaculty().getFirstName() + " " + course.getFaculty().getLastName() : "TBA"));
                faculty.getStyle().set("display", "block");
                
                Span credit = new Span("Credit Hours: " + course.getCreditHours());
                credit.getStyle().set("display", "block");
                
                courseCard.add(courseName, faculty, credit);
                recentCourses.add(courseCard);
            }
            
            add(recentCourses);
        }
    }
    
    private Component createStatCard(String title, String value, VaadinIcon icon, String iconColor) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle()
            .set("background-color", "var(--lumo-base-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)");
        
        Icon cardIcon = icon.create();
        cardIcon.setSize("40px");
        cardIcon.getStyle().set("color", iconColor);
        
        H2 valueText = new H2(value);
        valueText.getStyle().set("margin", "0");
        
        Span titleText = new Span(title);
        titleText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        card.add(cardIcon, valueText, titleText);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return card;
    }
    
    private Component createAttendanceCard(List<Course> courses) {
        // Get actual attendance data from service
        User student = securityService.getCurrentUser();
        double totalAttendance = 0.0;
        int totalCourses = courses.size();
        
        if (totalCourses > 0) {
            for (Course course : courses) {
                BigDecimal courseAttendance = attendanceService.calculateAttendancePercentage(student, course);
                totalAttendance += courseAttendance.doubleValue();
            }
            
            // Calculate average attendance across all courses
            totalAttendance = totalAttendance / totalCourses;
        }
        
        // Round to nearest integer
        int attendancePercentage = (int) Math.round(totalAttendance);
        
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle()
            .set("background-color", "var(--lumo-base-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)");
        
        Icon cardIcon = VaadinIcon.CLOCK.create();
        cardIcon.setSize("40px");
        
        // Set color based on percentage
        String iconColor;
        if (attendancePercentage < 70) {
            iconColor = "var(--lumo-error-color)";
        } else if (attendancePercentage < 80) {
            iconColor = "var(--lumo-warning-color)";
        } else {
            iconColor = "var(--lumo-success-color)";
        }
        cardIcon.getStyle().set("color", iconColor);
        
        H2 valueText = new H2(attendancePercentage + "%");
        valueText.getStyle().set("margin", "0");
        valueText.getStyle().set("color", iconColor);
        
        Span titleText = new Span("Attendance");
        titleText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        card.add(cardIcon, valueText, titleText);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return card;
    }
    
    private Component createExamCard(List<Course> courses) {
        // For demo, just generate a random number of upcoming exams
        Random random = new Random();
        int upcomingExams = random.nextInt(3); // 0-2
        
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle()
            .set("background-color", "var(--lumo-base-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)");
        
        Icon cardIcon = VaadinIcon.CALENDAR_CLOCK.create();
        cardIcon.setSize("40px");
        cardIcon.getStyle().set("color", "var(--lumo-primary-color)");
        
        H2 valueText = new H2(String.valueOf(upcomingExams));
        valueText.getStyle().set("margin", "0");
        
        Span titleText = new Span("Upcoming Exams");
        titleText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        card.add(cardIcon, valueText, titleText);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return card;
    }
    
    private Component createFeesCard() {
        // For demo, just generate a random fee status
        Random random = new Random();
        boolean feesPaid = random.nextBoolean();
        
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle()
            .set("background-color", "var(--lumo-base-color)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("box-shadow", "var(--lumo-box-shadow-xs)");
        
        Icon cardIcon = VaadinIcon.MONEY.create();
        cardIcon.setSize("40px");
        
        String iconColor = feesPaid ? "var(--lumo-success-color)" : "var(--lumo-error-color)";
        cardIcon.getStyle().set("color", iconColor);
        
        H2 valueText = new H2(feesPaid ? "Paid" : "Due");
        valueText.getStyle().set("margin", "0");
        valueText.getStyle().set("color", iconColor);
        
        Span titleText = new Span("Fee Status");
        titleText.getStyle().set("color", "var(--lumo-secondary-text-color)");
        
        card.add(cardIcon, valueText, titleText);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return card;
    }
    
    private Component createQuickLink(String title, VaadinIcon icon, String route) {
        VerticalLayout link = new VerticalLayout();
        link.setSpacing(false);
        link.setPadding(true);
        link.getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("cursor", "pointer")
            .set("transition", "background-color 0.3s");
        
        link.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(route)));
        
        // Hover effect - remove the setEventType calls that are causing errors
        link.getElement().addEventListener("mouseover", 
            e -> link.getStyle().set("background-color", "var(--lumo-contrast-10pct)"));
        link.getElement().addEventListener("mouseout", 
            e -> link.getStyle().set("background-color", "var(--lumo-contrast-5pct)"));
        
        Icon linkIcon = icon.create();
        linkIcon.setSize("24px");
        linkIcon.getStyle().set("color", "var(--lumo-primary-color)");
        
        Span linkText = new Span(title);
        
        link.add(linkIcon, linkText);
        link.setAlignItems(FlexComponent.Alignment.CENTER);
        link.setWidth("180px");
        
        return link;
    }
} 