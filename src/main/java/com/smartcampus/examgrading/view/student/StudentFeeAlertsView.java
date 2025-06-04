package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.FeeAlert;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.FeeService;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@PageTitle("Fee Alerts | Smart Campus")
@Route(value = "student/fee-alerts", layout = StudentView.class)
@RouteAlias(value = "student/alerts", layout = StudentView.class)
public class StudentFeeAlertsView extends VerticalLayout {

    private final FeeService feeService;
    private final SessionService sessionService;
    private final Grid<FeeAlert> alertsGrid;

    @Autowired
    public StudentFeeAlertsView(FeeService feeService, SessionService sessionService) {
        this.feeService = feeService;
        this.sessionService = sessionService;
        
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header
        H1 header = new H1("Fee Alerts");
        add(header);

        // Initialize grid
        alertsGrid = new Grid<>(FeeAlert.class);
        alertsGrid.setSizeFull();
        alertsGrid.setColumns("alertDate", "alertType", "message");
        alertsGrid.addColumn(alert -> alert.getStudentFee().getStudentFeeId())
                .setHeader("Fee ID");
        alertsGrid.getColumnByKey("alertDate").setHeader("Date");
        alertsGrid.getColumnByKey("alertType").setHeader("Type");
        alertsGrid.getColumnByKey("message").setHeader("Message");
        
        // Load alerts
        loadAlerts();
        
        add(alertsGrid);
    }

    private void loadAlerts() {
        User currentStudent = sessionService.getCurrentUser();
        if (currentStudent != null) {
            List<FeeAlert> alerts = feeService.getFeeAlertsForStudent(currentStudent);
            alertsGrid.setItems(alerts);
        }
    }
} 