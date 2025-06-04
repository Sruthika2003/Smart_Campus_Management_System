package com.smartcampus.examgrading.view.accounts;

import com.smartcampus.examgrading.model.StudentFee;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.FeeAlert;
import com.smartcampus.examgrading.service.AlertService;
import com.smartcampus.examgrading.service.FeeService;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "accounts/pending-fees", layout = AccountsView.class)
@PageTitle("Pending Fees | Accounts")
public class PendingFeesView extends VerticalLayout implements BeforeEnterObserver {
    private final FeeService feeService;
    private final SessionService sessionService;
    private final AlertService alertService;
    private User currentUser;

    private Grid<StudentFee> feeGrid = new Grid<>(StudentFee.class, false);
    private ComboBox<String> academicYearComboBox = new ComboBox<>("Select Academic Year");
    private ComboBox<String> semesterComboBox = new ComboBox<>("Select Semester");
    private Paragraph totalPendingAmount = new Paragraph();

    public PendingFeesView(FeeService feeService, SessionService sessionService, AlertService alertService) {
        this.feeService = feeService;
        this.sessionService = sessionService;
        this.alertService = alertService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        if (!sessionService.isLoggedIn()) {
            return; // BeforeEnter will handle the redirect
        }

        this.currentUser = sessionService.getCurrentUser();

        if (!sessionService.isAccounts()) {
            add(new H2("Access Denied"),
                    new Paragraph("Only accounts staff can access this page."));
            return;
        }

        setupLayout();
    }

    private void setupLayout() {
        add(new H2("Pending Fees Management"));

        // Create filter controls
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);

        // Setup academic year combo box
        academicYearComboBox.setItems("2023-24", "2024-25", "2025-26");
        academicYearComboBox.setValue(getCurrentAcademicYear());
        academicYearComboBox.addValueChangeListener(e -> updateGrid());

        // Setup semester combo box
        semesterComboBox.setItems("Semester 1", "Semester 2", "Semester 3", "Semester 4", "Semester 5", "Semester 6", "Semester 7", "Semester 8");
        semesterComboBox.addValueChangeListener(e -> updateGrid());

        filterLayout.add(academicYearComboBox, semesterComboBox);
        add(filterLayout);

        // Add summary
        add(totalPendingAmount);

        // Configure and add grid
        configureGrid();
        add(feeGrid);

        // Update grid with initial data
        updateGrid();
    }

    private void configureGrid() {
        feeGrid.removeAllColumns();

        // Student details
        feeGrid.addColumn(fee -> fee.getStudent().getFirstName() + " " + fee.getStudent().getLastName())
                .setHeader("Student Name")
                .setAutoWidth(true);

        feeGrid.addColumn(fee -> fee.getStudent().getEmail())
                .setHeader("Student Email")
                .setAutoWidth(true);

        // Fee details
        feeGrid.addColumn(fee -> fee.getFeeType().getFeeName())
                .setHeader("Fee Type")
                .setAutoWidth(true);

        feeGrid.addColumn(fee -> fee.getFeeType().getDescription())
                .setHeader("Description")
                .setAutoWidth(true);

        feeGrid.addColumn(fee -> fee.getAmount().toString())
                .setHeader("Amount")
                .setAutoWidth(true);

        feeGrid.addColumn(fee -> fee.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setHeader("Due Date")
                .setAutoWidth(true);

        feeGrid.addColumn(fee -> {
            long daysOverdue = LocalDate.now().toEpochDay() - fee.getDueDate().toEpochDay();
            return daysOverdue > 0 ? daysOverdue + " days" : "Not due";
        }).setHeader("Days Overdue")
          .setAutoWidth(true);

        // Add alert status and action column
        feeGrid.addColumn(new ComponentRenderer<>(fee -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setAlignItems(Alignment.CENTER);

            // Show alert status
            if (alertService.hasAlertBeenSent(fee)) {
                Span status = new Span("Alert Sent");
                status.getStyle().set("color", "var(--lumo-success-color)");
                layout.add(status);
            } else {
                Button alertButton = new Button("Generate Alert", e -> showAlertDialog(fee));
                alertButton.setThemeName("error");
                layout.add(alertButton);
            }

            return layout;
        })).setHeader("Actions").setAutoWidth(true);

        feeGrid.setHeight("100%");
    }

    private void showAlertDialog(StudentFee fee) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Fee Alert Preview");

        TextArea alertContent = new TextArea();
        alertContent.setValue(alertService.generateAlertMessage(fee));
        alertContent.setWidthFull();
        alertContent.setHeight("300px");
        alertContent.setReadOnly(true);

        Button sendButton = new Button("Send Alert", e -> {
            try {
                FeeAlert alert = alertService.generateFeeAlert(fee, currentUser);
                Notification.show(
                    "Alert sent successfully to " + fee.getStudent().getFirstName() + " " + 
                    fee.getStudent().getLastName(), 
                    3000, 
                    Notification.Position.MIDDLE
                );
                dialog.close();
                updateGrid(); // Refresh the grid to update the alert status
            } catch (IllegalStateException ex) {
                Notification.show(
                    ex.getMessage(), 
                    5000, 
                    Notification.Position.MIDDLE
                );
            } catch (Exception ex) {
                Notification.show(
                    "Error sending alert: " + ex.getMessage(), 
                    5000, 
                    Notification.Position.MIDDLE
                );
            }
        });
        sendButton.setThemeName("primary");

        Button cancelButton = new Button("Cancel", e -> dialog.close());

        dialog.add(alertContent);
        dialog.getFooter().add(cancelButton, sendButton);
        dialog.open();
    }

    private void updateGrid() {
        List<StudentFee> pendingFees = feeService.getPendingFees(
            academicYearComboBox.getValue(),
            semesterComboBox.getValue()
        );
        feeGrid.setItems(pendingFees);

        // Update summary
        BigDecimal totalAmount = pendingFees.stream()
                .map(StudentFee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalPendingAmount.setText("Total Pending Amount: ₹" + totalAmount + 
                                " | Number of Pending Fees: " + pendingFees.size());
    }

    private String getCurrentAcademicYear() {
        int currentYear = LocalDate.now().getYear();
        return currentYear + "-" + (currentYear + 1);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.isLoggedIn()) {
            event.rerouteTo("login");
        }
    }
} 