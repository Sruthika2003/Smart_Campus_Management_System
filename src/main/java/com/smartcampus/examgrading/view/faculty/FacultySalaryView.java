package com.smartcampus.examgrading.view.faculty;

import com.smartcampus.examgrading.model.FacultySalary;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.FacultySalaryService;
import com.smartcampus.examgrading.service.SessionService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
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
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "faculty/salary", layout = FacultyView.class)
@PageTitle("Salary Details | Faculty")
public class FacultySalaryView extends VerticalLayout implements BeforeEnterObserver {
    private final FacultySalaryService facultySalaryService;
    private final SessionService sessionService;
    private User currentUser;

    private Grid<FacultySalary> salaryGrid = new Grid<>(FacultySalary.class, false);
    private DatePicker startDatePicker = new DatePicker("Start Date");
    private DatePicker endDatePicker = new DatePicker("End Date");
    private Paragraph totalSalaries = new Paragraph();

    public FacultySalaryView(FacultySalaryService facultySalaryService, SessionService sessionService) {
        this.facultySalaryService = facultySalaryService;
        this.sessionService = sessionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        if (!sessionService.isLoggedIn()) {
            return; // BeforeEnter will handle the redirect
        }

        this.currentUser = sessionService.getCurrentUser();

        if (!sessionService.isFaculty()) {
            add(new H2("Access Denied"),
                    new Paragraph("Only faculty members can access this page."));
            return;
        }

        setupLayout();
    }

    private void setupLayout() {
        add(new H2("My Salary Details"));

        // Create filter controls
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);

        // Setup date pickers
        startDatePicker.setValue(LocalDate.now().minusMonths(6));
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.addValueChangeListener(e -> updateGrid());
        endDatePicker.addValueChangeListener(e -> updateGrid());

        filterLayout.add(startDatePicker, endDatePicker);
        add(filterLayout);

        // Add salary summary
        add(totalSalaries);

        // Configure and add grid
        configureGrid();
        add(salaryGrid);

        // Update grid with initial data
        updateGrid();
    }

    private void configureGrid() {
        salaryGrid.setSizeFull();
        salaryGrid.addColumn(FacultySalary::getSalaryDate).setHeader("Salary Date").setSortable(true);
        salaryGrid.addColumn(FacultySalary::getAmount).setHeader("Amount").setSortable(true);
        salaryGrid.addColumn(FacultySalary::getPaymentMethod).setHeader("Payment Method").setSortable(true);
        salaryGrid.addColumn(FacultySalary::getTransactionReference).setHeader("Transaction Reference");
        salaryGrid.addColumn(FacultySalary::getCreditedAt).setHeader("Credited Date").setSortable(true);
        salaryGrid.addColumn(salary -> {
            User creditedBy = salary.getCreditedBy();
            return creditedBy != null ? creditedBy.getFirstName() + " " + creditedBy.getLastName() : "";
        }).setHeader("Credited By");
        salaryGrid.addColumn(FacultySalary::getRemarks).setHeader("Remarks");

        // Add receipt download column
        salaryGrid.addColumn(new ComponentRenderer<>(salary -> {
            Button downloadButton = new Button("Download Receipt", new Icon(VaadinIcon.DOWNLOAD));
            downloadButton.addClickListener(e -> downloadReceipt(salary));
            return downloadButton;
        })).setHeader("Receipt");
    }

    private void downloadReceipt(FacultySalary salary) {
        // Generate receipt content
        String receiptContent = generateReceiptContent(salary);
        
        // Create a stream resource for the receipt
        StreamResource resource = new StreamResource(
            "salary_receipt_" + salary.getSalaryDate().format(DateTimeFormatter.ofPattern("yyyy_MM")) + ".txt",
            () -> new ByteArrayInputStream(receiptContent.getBytes())
        );

        // Create a download link
        Anchor downloadLink = new Anchor(resource, "");
        downloadLink.getElement().setAttribute("download", true);
        
        // Add the link to the UI
        add(downloadLink);
        
        // Trigger the download
        downloadLink.getElement().executeJs("this.click()");
        
        // Remove the link after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(1000); // Wait for 1 second
                getUI().ifPresent(ui -> ui.access(() -> remove(downloadLink)));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String generateReceiptContent(FacultySalary salary) {
        StringBuilder content = new StringBuilder();
        content.append("==========================================\n");
        content.append("           SALARY RECEIPT\n");
        content.append("==========================================\n\n");
        content.append("Faculty Name: ").append(salary.getFaculty().getFirstName())
               .append(" ").append(salary.getFaculty().getLastName()).append("\n");
        content.append("Salary Date: ").append(salary.getSalaryDate()).append("\n");
        content.append("Amount: ₹").append(salary.getAmount()).append("\n");
        content.append("Payment Method: ").append(salary.getPaymentMethod()).append("\n");
        content.append("Transaction Reference: ").append(salary.getTransactionReference()).append("\n");
        content.append("Status: ").append(salary.getStatus()).append("\n");
        content.append("Credited By: ").append(salary.getCreditedBy().getFirstName())
               .append(" ").append(salary.getCreditedBy().getLastName()).append("\n");
        content.append("Credited At: ").append(salary.getCreditedAt()).append("\n");
        content.append("Remarks: ").append(salary.getRemarks()).append("\n\n");
        content.append("==========================================\n");
        content.append("This is a computer-generated receipt\n");
        content.append("==========================================");
        
        return content.toString();
    }

    private void updateGrid() {
        List<FacultySalary> salaries = facultySalaryService.getFacultySalariesByDateRange(
                currentUser,
                startDatePicker.getValue(),
                endDatePicker.getValue()
        );
        
        // Filter to show only credited salaries
        List<FacultySalary> creditedSalaries = salaries.stream()
                .filter(salary -> salary.getStatus() == FacultySalary.Status.PAID)
                .toList();
                
        salaryGrid.setItems(creditedSalaries);

        // Update summary for credited salaries only
        double totalAmount = creditedSalaries.stream()
                .mapToDouble(salary -> salary.getAmount().doubleValue())
                .sum();

        totalSalaries.setText(String.format("Total Credited Salaries: ₹%.2f | Number of Records: %d", 
                            totalAmount, creditedSalaries.size()));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.isLoggedIn()) {
            event.rerouteTo("login");
        }
    }
} 