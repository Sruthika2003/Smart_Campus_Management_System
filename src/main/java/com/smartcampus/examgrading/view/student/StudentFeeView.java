package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.*;
import com.smartcampus.examgrading.service.FeeService;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("student/fees")
@PageTitle("Fee Management | Student")
public class StudentFeeView extends VerticalLayout implements BeforeEnterObserver {
    private final FeeService feeService;
    private final SessionService sessionService;
    private User currentUser;

    private Grid<StudentFee> feeGrid = new Grid<>(StudentFee.class, false);
    private ComboBox<String> semesterComboBox = new ComboBox<>("Select Semester");
    private ComboBox<String> academicYearComboBox = new ComboBox<>("Select Academic Year");
    private Paragraph totalPendingFees = new Paragraph();
    private Paragraph totalPaidFees = new Paragraph();

    public StudentFeeView(FeeService feeService, SessionService sessionService) {
        this.feeService = feeService;
        this.sessionService = sessionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        if (!sessionService.isLoggedIn()) {
            return; // BeforeEnter will handle the redirect
        }

        this.currentUser = sessionService.getCurrentUser();

        if (!sessionService.isStudent()) {
            add(new H2("Access Denied"),
                    new Paragraph("Only students can access this page."));
            return;
        }

        setupLayout();
    }

    private void setupLayout() {
        add(new H2("Fee Management"));

        // Create filter controls
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);

        // Setup semester combo box
        semesterComboBox.setItems("Semester 1", "Semester 2");
        semesterComboBox.setValue(getCurrentSemester());
        semesterComboBox.addValueChangeListener(e -> updateGrid());

        // Setup academic year combo box
        academicYearComboBox.setItems("2023-24", "2024-25", "2025-26");
        academicYearComboBox.setValue(getCurrentAcademicYear());
        academicYearComboBox.addValueChangeListener(e -> updateGrid());

        filterLayout.add(semesterComboBox, academicYearComboBox);
        add(filterLayout);

        // Add fee summary
        HorizontalLayout summaryLayout = new HorizontalLayout();
        summaryLayout.setWidthFull();
        summaryLayout.setSpacing(true);
        summaryLayout.add(totalPendingFees, totalPaidFees);
        add(summaryLayout);

        // Configure and add grid
        configureGrid();
        add(feeGrid);

        // Update grid with initial data
        updateGrid();
    }

    private void configureGrid() {
        feeGrid.removeAllColumns();

        // Add Fee ID column
        feeGrid.addColumn(StudentFee::getStudentFeeId)
                .setHeader("Fee ID")
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

        feeGrid.addColumn(StudentFee::getStatus)
                .setHeader("Status")
                .setAutoWidth(true);

        // Add pay button for pending fees
        feeGrid.addColumn(new ComponentRenderer<>(fee -> {
            if (fee.getStatus() == StudentFee.Status.PENDING) {
                Button payButton = new Button("Pay", e -> openPaymentDialog(fee));
                payButton.setThemeName("primary");
                return payButton;
            }
            return null;
        })).setHeader("Actions").setAutoWidth(true);

        feeGrid.setHeight("100%");
    }

    private void openPaymentDialog(StudentFee fee) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Make Payment");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();

        // Payment method selection
        ComboBox<Payment.PaymentMethod> paymentMethodComboBox = new ComboBox<>("Payment Method");
        paymentMethodComboBox.setItems(Payment.PaymentMethod.values());
        paymentMethodComboBox.setRequired(true);

        // Remarks
        TextArea remarksField = new TextArea("Remarks");
        remarksField.setWidthFull();

        formLayout.add(paymentMethodComboBox, remarksField);
        dialog.add(formLayout);

        // Buttons
        Button payButton = new Button("Pay", e -> {
            if (paymentMethodComboBox.isEmpty()) {
                Notification.show("Please select a payment method", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                Payment payment = feeService.processPayment(
                        fee,
                        paymentMethodComboBox.getValue(),
                        remarksField.getValue()
                );

                Notification.show("Payment successful! Receipt Number: " + payment.getReceiptNumber() + 
                                "\nTransaction Reference: " + payment.getTransactionReference(),
                        5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                dialog.close();
                updateGrid();
            } catch (Exception ex) {
                Notification.show("Error processing payment: " + ex.getMessage(),
                        3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton, payButton);

        dialog.open();
    }

    private void updateGrid() {
        List<StudentFee> fees = feeService.getStudentFeesBySemester(
                currentUser,
                semesterComboBox.getValue(),
                academicYearComboBox.getValue()
        );

        feeGrid.setItems(fees);

        // Update summary - calculate totals only for the filtered fees
        BigDecimal totalPending = fees.stream()
                .filter(fee -> fee.getStatus() == StudentFee.Status.PENDING)
                .map(StudentFee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = fees.stream()
                .filter(fee -> fee.getStatus() == StudentFee.Status.PAID)
                .map(StudentFee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalPendingFees.setText("Total Pending Fees: ₹" + totalPending);
        totalPaidFees.setText("Total Paid Fees: ₹" + totalPaid);
    }

    private String getCurrentSemester() {
        Month currentMonth = LocalDate.now().getMonth();
        if (currentMonth.getValue() >= Month.AUGUST.getValue() && 
            currentMonth.getValue() <= Month.DECEMBER.getValue()) {
            return "Semester 1";
        } else {
            return "Semester 2";
        }
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