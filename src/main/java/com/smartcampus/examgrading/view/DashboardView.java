package com.smartcampus.examgrading.view;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("dashboard")
@PageTitle("Dashboard | Smart Campus System")
public class DashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final SessionService sessionService;
    private User currentUser;

    public DashboardView(SessionService sessionService) {
        this.sessionService = sessionService;
        this.currentUser = sessionService.getCurrentUser();

        // Set layout properties
        setSizeFull();
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setPadding(true);
        setSpacing(true);
        getStyle().set("background-color", "#f5f5f5");

        // Check if user is logged in
        if (!sessionService.isLoggedIn()) {
            return; // BeforeEnter will handle the redirect
        }

        setupDashboard();
    }

    private void setupDashboard() {
        // Create header
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setPadding(true);
        headerLayout.setSpacing(true);
        headerLayout.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
                .set("margin-bottom", "20px");

        // Add logo
        Image logo = new Image("images/logo.png", "Smart Campus Logo");
        logo.setHeight("64px");

        // Welcome message
        VerticalLayout welcomeLayout = new VerticalLayout();
        welcomeLayout.setPadding(false);
        welcomeLayout.setSpacing(false);

        H2 welcomeHeader = new H2("Welcome to Smart Campus");
        Paragraph welcomeText = new Paragraph(
                "Hello, " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");

        welcomeLayout.add(welcomeHeader, welcomeText);

        // Logout button
        Button logoutButton = new Button("Logout", e -> {
            sessionService.logout();
            UI.getCurrent().navigate(LoginView.class);
        });
        logoutButton.getStyle()
                .set("margin-left", "auto")
                .set("background-color", "#f44336")
                .set("color", "white");

        headerLayout.add(logo, welcomeLayout, logoutButton);
        add(headerLayout);

        // Main content - Navigation cards
        HorizontalLayout cardLayout = new HorizontalLayout();
        cardLayout.setWidthFull();
        cardLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        cardLayout.setSpacing(true);
        cardLayout.getStyle().set("flex-wrap", "wrap"); // Replaces setWrapping(true)

        // Add different navigation options based on user role
        if (sessionService.isStudent()) {
            // Student navigation options
            cardLayout.add(createNavigationCard(
                    "My Exams",
                    "View your upcoming and past exams",
                    "student-exams"));

            cardLayout.add(createNavigationCard(
                    "My Grades",
                    "View your grades and performance analytics",
                    "student-grades"));

            // Additional student cards could be added here
            cardLayout.add(createNavigationCard(
                    "Course Materials",
                    "Access your course materials and resources",
                    "student-materials"));
                    
            // Add Attendance card for students
            cardLayout.add(createNavigationCard(
                    "My Attendance",
                    "View your attendance records and submit correction requests",
                    "student-attendance"));

        } else if (sessionService.isFaculty()) {
            // Faculty navigation options
            cardLayout.add(createNavigationCard(
                    "Exam Management",
                    "Schedule and manage exams",
                    "exams"));

            cardLayout.add(createNavigationCard(
                    "Exam Papers",
                    "Upload and manage exam papers",
                    "exam-papers"));

            cardLayout.add(createNavigationCard(
                    "Grade Management",
                    "Enter and manage student grades",
                    "grade-management"));
                    
            // Add Attendance Management card for faculty
            cardLayout.add(createNavigationCard(
                    "Attendance Management",
                    "Mark and manage student attendance",
                    "faculty-attendance"));
                    
            // Add Attendance Correction Requests card for faculty
            cardLayout.add(createNavigationCard(
                    "Attendance Corrections",
                    "Review student attendance correction requests",
                    "attendance-corrections"));

            // Add system announcements section - Only for Faculty
            VerticalLayout announcementsLayout = new VerticalLayout();
            announcementsLayout.setWidthFull();
            announcementsLayout.setPadding(true);
            announcementsLayout.getStyle()
                    .set("background-color", "white")
                    .set("border-radius", "8px")
                    .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)")
                    .set("margin-top", "20px");

            H2 announcementsHeader = new H2("System Announcements");
            Paragraph announcement = new Paragraph(
                    "All exam grades for Spring 2025 must be submitted by May 15, 2025.");

            announcementsLayout.add(announcementsHeader, announcement);
            add(announcementsLayout);

        } else if (sessionService.isAdmin()) {
            // Admin navigation options
            cardLayout.add(createNavigationCard(
                    "Exam Management",
                    "Schedule and manage exams",
                    "exams"));
                    
            // Add Attendance Reports card for admin
            cardLayout.add(createNavigationCard(
                    "Attendance Reports",
                    "View and generate attendance reports for all courses",
                    "admin-attendance-reports"));
        }

        add(cardLayout);
    }

    private VerticalLayout createNavigationCard(String title, String description, String route) {
        VerticalLayout card = new VerticalLayout();
        card.setWidth("300px");
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
                .set("background-color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0 4px 8px rgba(0, 0, 0, 0.1)")
                .set("transition", "transform 0.3s")
                .set("cursor", "pointer")
                .set("margin", "10px");

        // Add hover effect with CSS
        card.getElement().addEventListener("mouseover",
                e -> card.getStyle().set("transform", "translateY(-5px)"));
        card.getElement().addEventListener("mouseout",
                e -> card.getStyle().set("transform", "translateY(0)"));

        H2 cardTitle = new H2(title);
        cardTitle.getStyle().set("margin-top", "0");

        Paragraph cardDesc = new Paragraph(description);

        Button navigateButton = new Button("Go to " + title, e -> UI.getCurrent().navigate(route));
        navigateButton.getStyle()
                .set("background-color", "#1565C0")
                .set("color", "white")
                .set("margin-top", "auto");

        card.add(cardTitle, cardDesc, navigateButton);

        // Make the entire card clickable
        card.addClickListener(e -> UI.getCurrent().navigate(route));

        return card;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Redirect to login if not logged in
        if (!sessionService.isLoggedIn()) {
            event.forwardTo(LoginView.class);
        }
    }
}