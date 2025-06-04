package com.smartcampus.examgrading.view;

import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.security.SecurityService;
import com.smartcampus.examgrading.view.admin.AdminView;
import com.smartcampus.examgrading.view.faculty.FacultyView;
import com.smartcampus.examgrading.view.student.*;
import com.smartcampus.examgrading.view.faculty.*;
import com.smartcampus.examgrading.view.admin.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {
    private final SecurityService securityService;

    public MainLayout(SecurityService securityService) {
        this.securityService = securityService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Smart Campus System");
        logo.addClassNames("text-l", "m-m");

        Button logout = new Button("Log out", e -> {
            securityService.logout();
            UI.getCurrent().navigate(LoginView.class);
        });

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, logout);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);

        if (securityService.hasRole(User.Role.STUDENT)) {
            // Course Management
            tabs.add(createTab(VaadinIcon.HOME, "Dashboard", StudentDashboardView.class));
            tabs.add(createTab(VaadinIcon.ACADEMY_CAP, "Available Courses", AvailableCoursesView.class));
            tabs.add(createTab(VaadinIcon.LIST, "My Courses", MyCoursesView.class));
            tabs.add(createTab(VaadinIcon.CALENDAR, "Timetable", TimetableView.class));

            // Exam Management
            tabs.add(createTab(VaadinIcon.CALENDAR_CLOCK, "Exam Schedule", StudentExamScheduleView.class));
            tabs.add(createTab(VaadinIcon.CHART, "Exam Results", StudentExamResultView.class));

            // Grade Management
            tabs.add(createTab(VaadinIcon.RECORDS, "My Grades", StudentGradeView.class));
            
            // Attendance Management - Add dedicated section with icon
            Div attendanceSection = new Div();
            attendanceSection.getStyle().set("padding-left", "var(--lumo-space-m)")
                             .set("font-weight", "bold")
                             .set("margin-top", "var(--lumo-space-m)")
                             .set("margin-bottom", "var(--lumo-space-xs)")
                             .set("color", "var(--lumo-tertiary-text-color)");
            attendanceSection.add(new H4("Attendance"));
            addToDrawer(attendanceSection);
            
            // Add the three attendance tabs
            tabs.add(createTab(VaadinIcon.CLOCK, "View Attendance", StudentAttendanceView.class));
            tabs.add(createTab(VaadinIcon.ENVELOPE, "Request Correction", StudentAttendanceCorrectionView.class));
            tabs.add(createTab(VaadinIcon.BELL, "Attendance Alerts", StudentAttendanceAlertsView.class));

            // Fee Management
            tabs.add(createTab(VaadinIcon.MONEY, "Fee Management", StudentFeeView.class));
            tabs.add(createTab(VaadinIcon.WALLET, "Payment History", StudentPaymentHistoryView.class));
            
            // Profile
            tabs.add(createTab(VaadinIcon.USER, "My Profile", MyProfileView.class));

        } else if (securityService.hasRole(User.Role.FACULTY)) {
            // Course Management
            tabs.add(createTab(VaadinIcon.ACADEMY_CAP, "My Courses", CoursesView.class));
            tabs.add(createTab(VaadinIcon.USERS, "Students", StudentsView.class));
            tabs.add(createTab(VaadinIcon.CALENDAR, "Timetable", FacultyTimetableView.class));
            
            // Exam Management
            tabs.add(createTab(VaadinIcon.EDIT, "Exam Management", com.smartcampus.examgrading.view.faculty.ExamView.class));
            tabs.add(createTab(VaadinIcon.UPLOAD, "Exam Papers", ExamPaperView.class));
            
            // Grade Management
            tabs.add(createTab(VaadinIcon.RECORDS, "Grade Management", FacultyGradeView.class));
            
            // Attendance Management
            tabs.add(createTab(VaadinIcon.CLOCK, "Attendance Management", FacultyAttendanceView.class));

            // Salary Management
            tabs.add(createTab(VaadinIcon.MONEY, "Salary Details", FacultySalaryView.class));

        } else if (securityService.hasRole(User.Role.ADMIN)) {
            // Course Management
            tabs.add(createTab(VaadinIcon.ACADEMY_CAP, "Courses", CoursesManagementView.class));
            tabs.add(createTab(VaadinIcon.USERS, "Users", UsersView.class));
            tabs.add(createTab(VaadinIcon.CALENDAR, "Timetable", TimetableManagementView.class));
            
            // Exam Management
            tabs.add(createTab(VaadinIcon.EDIT, "Exam Management", com.smartcampus.examgrading.view.admin.ExamView.class));
            
            // Attendance Management
            tabs.add(createTab(VaadinIcon.CLOCK, "Attendance Reports", AdminAttendanceReportsView.class));
        }

        addToDrawer(tabs);
    }

    private <T extends Component> Tab createTab(VaadinIcon viewIcon, String viewName, Class<T> viewClass) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(viewName));
        link.setRoute(viewClass);
        link.setTabIndex(-1);

        return new Tab(link);
    }
}