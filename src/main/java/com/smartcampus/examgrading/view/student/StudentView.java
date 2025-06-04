package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.UI;

@Route(value = "student", layout = MainLayout.class)
@PageTitle("Student Dashboard | Course Management System")
public class StudentView extends AppLayout implements RouterLayout {

    public StudentView() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        H1 title = new H1("Student Dashboard");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        // Create logout button
        Button logoutButton = new Button("Logout", new Icon(VaadinIcon.SIGN_OUT));
        logoutButton.addClickListener(e -> {
            VaadinSession.getCurrent().getSession().invalidate();
            UI.getCurrent().navigate("login");
        });
        logoutButton.getStyle()
                .set("margin-left", "auto")
                .set("margin-right", "1rem");
        
        HorizontalLayout header = new HorizontalLayout(toggle, title, logoutButton);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.addClassName("header");
        header.expand(title); // This will push the logout button to the right
        
        addToNavbar(header);
    }
    
    private void createDrawer() {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.add(
                createTab(VaadinIcon.HOME, "Dashboard", StudentDashboardView.class),
                createTab(VaadinIcon.BOOK, "My Courses", MyCoursesView.class),
                createTab(VaadinIcon.BOOKMARK, "Available Courses", AvailableCoursesView.class),
                createTab(VaadinIcon.CALENDAR, "Exam Schedule", StudentExamScheduleView.class),
                createTab(VaadinIcon.CHART, "Grades", StudentGradeView.class),
                createTab(VaadinIcon.CREDIT_CARD, "Fee Management", StudentFeeView.class),
                createTab(VaadinIcon.BELL, "Fee Alerts", StudentFeeAlertsView.class),
                createTab(VaadinIcon.WALLET, "Payment History", StudentPaymentHistoryView.class),
                createTab(VaadinIcon.USER, "My Profile", StudentProfileView.class)
        );
        
        addToDrawer(tabs);
    }
    
    private Tab createTab(VaadinIcon icon, String title, Class<? extends VerticalLayout> viewClass) {
        Icon iconComponent = icon.create();
        iconComponent.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("margin-inline-start", "var(--lumo-space-xs)")
                .set("padding", "var(--lumo-space-xs)");
        
        RouterLink link = new RouterLink();
        link.add(new Span(iconComponent), new Span(title));
        link.setRoute(viewClass);
        link.setTabIndex(-1);
        
        return new Tab(link);
    }
}