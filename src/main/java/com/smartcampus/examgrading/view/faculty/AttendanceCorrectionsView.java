package com.smartcampus.examgrading.view.faculty;

import com.smartcampus.examgrading.model.*;
import com.smartcampus.examgrading.service.*;
import com.smartcampus.examgrading.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "attendance-corrections", layout = MainLayout.class)
@PageTitle("Attendance Correction Requests | Smart Campus")
public class AttendanceCorrectionsView extends VerticalLayout implements BeforeEnterObserver {

    private final AttendanceService attendanceService;
    private final SessionService sessionService;
    private final User currentUser;
    
    private Grid<AttendanceCorrectionRequest> requestsGrid = new Grid<>(AttendanceCorrectionRequest.class);

    public AttendanceCorrectionsView(
            AttendanceService attendanceService,
            SessionService sessionService) {
        this.attendanceService = attendanceService;
        this.sessionService = sessionService;
        this.currentUser = sessionService.getCurrentUser();

        setPadding(true);
        setSpacing(true);
        setWidthFull();

        H2 viewTitle = new H2("Attendance Correction Requests");
        viewTitle.addClassNames(Padding.Top.MEDIUM);
        add(viewTitle);

        setupRequestsGrid();
    }

    private void setupRequestsGrid() {
        VerticalLayout gridLayout = new VerticalLayout();
        gridLayout.setPadding(false);
        gridLayout.setSpacing(true);
        gridLayout.setWidthFull();

        H3 sectionTitle = new H3("Pending Requests");
        gridLayout.add(sectionTitle);

        // Configure grid
        requestsGrid.setWidthFull();
        requestsGrid.setHeight("500px");
        
        // Remove default columns
        requestsGrid.removeAllColumns();
        
        // Add columns
        requestsGrid.addColumn(request -> 
                request.getAttendance().getAttendanceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setHeader("Date")
                .setSortable(true);
        
        requestsGrid.addColumn(request -> 
                request.getAttendance().getCourse().getCourseName())
                .setHeader("Course")
                .setSortable(true);
        
        requestsGrid.addColumn(request -> 
                request.getRequestedBy().getFirstName() + " " + request.getRequestedBy().getLastName())
                .setHeader("Student")
                .setSortable(true);
        
        requestsGrid.addColumn(AttendanceCorrectionRequest::getReason)
                .setHeader("Reason");
                
        requestsGrid.addColumn(request -> 
                request.getRequestedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setHeader("Requested Date")
                .setSortable(true);
        
        // Add action buttons
        requestsGrid.addComponentColumn(request -> {
            HorizontalLayout actionsLayout = new HorizontalLayout();
            
            Button approveButton = new Button("Approve", e -> {
                showReviewDialog(request, AttendanceCorrectionRequest.RequestStatus.APPROVED);
            });
            approveButton.getStyle().set("color", "white").set("background-color", "green");
            
            Button rejectButton = new Button("Reject", e -> {
                showReviewDialog(request, AttendanceCorrectionRequest.RequestStatus.REJECTED);
            });
            rejectButton.getStyle().set("color", "white").set("background-color", "red");
            
            actionsLayout.add(approveButton, rejectButton);
            return actionsLayout;
        }).setHeader("Actions").setWidth("200px").setFlexGrow(0);
        
        // Add refresh button
        Button refreshButton = new Button("Refresh", e -> loadRequests());
        
        gridLayout.add(requestsGrid, refreshButton);
        add(gridLayout);
        
        // Load data
        loadRequests();
    }

    private void loadRequests() {
        List<AttendanceCorrectionRequest> pendingRequests = 
            attendanceService.getPendingRequestsForFaculty(currentUser);
        
        requestsGrid.setItems(pendingRequests);
        
        if (pendingRequests.isEmpty()) {
            Notification.show("No pending correction requests");
        }
    }
    
    private void showReviewDialog(AttendanceCorrectionRequest request, 
                                 AttendanceCorrectionRequest.RequestStatus status) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(true);
        dialogLayout.setSpacing(true);
        
        String actionText = (status == AttendanceCorrectionRequest.RequestStatus.APPROVED) 
            ? "Approve" : "Reject";
        
        H3 dialogTitle = new H3(actionText + " Attendance Correction Request");
        
        // Display request details
        Paragraph studentInfo = new Paragraph("Student: " 
            + request.getRequestedBy().getFirstName() + " " 
            + request.getRequestedBy().getLastName());
            
        Paragraph courseInfo = new Paragraph("Course: " 
            + request.getAttendance().getCourse().getCourseName());
            
        Paragraph dateInfo = new Paragraph("Date: " 
            + request.getAttendance().getAttendanceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        
        Paragraph reasonInfo = new Paragraph("Reason: " + request.getReason());
        
        TextArea commentsField = new TextArea("Review Comments");
        commentsField.setWidthFull();
        commentsField.setMinHeight("100px");
        
        Button confirmButton = new Button(actionText, e -> {
            String comments = commentsField.getValue();
            try {
                attendanceService.reviewCorrectionRequest(
                    request.getRequestId(), 
                    status, 
                    currentUser, 
                    comments
                );
                
                Notification.show("Request " + actionText.toLowerCase() + "d successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                loadRequests();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Failed to process request: " + ex.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        confirmButton.getStyle().set("margin-top", "20px");
        
        if (status == AttendanceCorrectionRequest.RequestStatus.APPROVED) {
            confirmButton.getStyle().set("background-color", "green").set("color", "white");
        } else {
            confirmButton.getStyle().set("background-color", "red").set("color", "white");
        }
        
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, confirmButton);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(JustifyContentMode.END);
        
        dialogLayout.add(
            dialogTitle,
            studentInfo,
            courseInfo,
            dateInfo,
            reasonInfo,
            commentsField,
            buttonLayout
        );
        
        dialog.add(dialogLayout);
        dialog.open();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.isLoggedIn() || !sessionService.isFaculty()) {
            event.forwardTo("login");
        }
    }
} 