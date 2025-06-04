package com.smartcampus.examgrading.view.student;

import com.smartcampus.examgrading.model.Payment;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.FeeService;
import com.smartcampus.examgrading.service.ReceiptService;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.html.Anchor;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("student/payment-history")
@PageTitle("Payment History | Student")
public class StudentPaymentHistoryView extends VerticalLayout implements BeforeEnterObserver {
    private final FeeService feeService;
    private final SessionService sessionService;
    private final ReceiptService receiptService;
    private User currentUser;

    private Grid<Payment> paymentGrid = new Grid<>(Payment.class, false);
    private ComboBox<String> academicYearComboBox = new ComboBox<>("Select Academic Year");
    private Paragraph totalPayments = new Paragraph();

    public StudentPaymentHistoryView(FeeService feeService, SessionService sessionService, ReceiptService receiptService) {
        this.feeService = feeService;
        this.sessionService = sessionService;
        this.receiptService = receiptService;

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
        add(new H2("Payment History"));

        // Create filter controls
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);

        // Setup academic year combo box
        academicYearComboBox.setItems("2023-24", "2024-25", "2025-26");
        academicYearComboBox.setValue(getCurrentAcademicYear());
        academicYearComboBox.addValueChangeListener(e -> updateGrid());

        filterLayout.add(academicYearComboBox);
        add(filterLayout);

        // Add payment summary
        add(totalPayments);

        // Configure and add grid
        configureGrid();
        add(paymentGrid);

        // Update grid with initial data
        updateGrid();
    }

    private void configureGrid() {
        paymentGrid.removeAllColumns();

        // Fee details
        paymentGrid.addColumn(payment -> payment.getStudentFee().getFeeType().getFeeName())
                .setHeader("Fee Type")
                .setAutoWidth(true);

        paymentGrid.addColumn(payment -> payment.getStudentFee().getFeeType().getDescription())
                .setHeader("Description")
                .setAutoWidth(true);

        paymentGrid.addColumn(payment -> payment.getAmount().toString())
                .setHeader("Amount")
                .setAutoWidth(true);

        paymentGrid.addColumn(payment -> payment.getPaymentDate().toLocalDateTime()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .setHeader("Payment Date")
                .setAutoWidth(true);

        paymentGrid.addColumn(Payment::getPaymentMethod)
                .setHeader("Payment Method")
                .setAutoWidth(true);

        paymentGrid.addColumn(Payment::getTransactionReference)
                .setHeader("Transaction Reference")
                .setAutoWidth(true);

        paymentGrid.addColumn(Payment::getReceiptNumber)
                .setHeader("Receipt Number")
                .setAutoWidth(true);

        // Add download receipt button
        paymentGrid.addColumn(new ComponentRenderer<>(payment -> {
            Button downloadButton = new Button("Download Receipt", e -> downloadReceipt(payment));
            downloadButton.setThemeName("primary");
            return downloadButton;
        })).setHeader("Actions").setAutoWidth(true);

        paymentGrid.setHeight("100%");
    }

    private void downloadReceipt(Payment payment) {
        try {
            // Generate PDF
            byte[] pdfBytes = receiptService.generateReceiptPdf(payment);
            
            // Create a stream resource for the PDF
            StreamResource resource = new StreamResource(
                "receipt_" + payment.getReceiptNumber() + ".pdf",
                () -> new ByteArrayInputStream(pdfBytes)
            );
            
            // Create a hidden anchor element
            Anchor downloadLink = new Anchor(resource, "");
            downloadLink.getElement().setAttribute("download", true);
            downloadLink.getElement().setAttribute("style", "display: none");
            add(downloadLink);
            
            // Programmatically click the link
            downloadLink.getElement().executeJs("this.click()");
            
            // Remove the link after download starts
            downloadLink.getElement().executeJs("setTimeout(() => this.remove(), 100)");
            
            // Show success notification
            Notification.show("Receipt downloaded successfully!", 3000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("Error downloading receipt: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void updateGrid() {
        List<Payment> payments = feeService.getStudentPayments(currentUser);
        paymentGrid.setItems(payments);

        // Update summary
        BigDecimal totalAmount = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalPayments.setText("Total Payments: ₹" + totalAmount + 
                            " | Number of Payments: " + payments.size());
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