package com.smartcampus.examgrading.view.accounts;

import com.smartcampus.examgrading.model.FacultySalary;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.FacultySalaryService;
import com.smartcampus.examgrading.service.SessionService;
import com.smartcampus.examgrading.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Route(value = "accounts/faculty-salaries", layout = AccountsView.class)
@PageTitle("Faculty Salaries | Accounts")
public class FacultySalaryView extends VerticalLayout implements BeforeEnterObserver {
    private final FacultySalaryService facultySalaryService;
    private final SessionService sessionService;
    private final UserService userService;
    private User currentUser;

    private Grid<FacultySalary> salaryGrid = new Grid<>(FacultySalary.class, false);
    private ComboBox<User> facultyComboBox = new ComboBox<>("Select Faculty");
    private DatePicker startDatePicker = new DatePicker("Start Date");
    private DatePicker endDatePicker = new DatePicker("End Date");
    private Paragraph totalSalaries = new Paragraph();

    public FacultySalaryView(FacultySalaryService facultySalaryService, SessionService sessionService,
                           UserService userService) {
        this.facultySalaryService = facultySalaryService;
        this.sessionService = sessionService;
        this.userService = userService;

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
        add(new H2("Faculty Salary Management"));

        // Create filter controls
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setSpacing(true);

        // Setup faculty combo box
        List<User> facultyList = userService.getUsersByRole(User.Role.FACULTY);
        facultyComboBox.setItems(facultyList);
        facultyComboBox.setItemLabelGenerator(user -> user.getFirstName() + " " + user.getLastName());
        facultyComboBox.addValueChangeListener(e -> updateGrid());

        // Setup date pickers
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        endDatePicker.setValue(LocalDate.now());
        startDatePicker.addValueChangeListener(e -> updateGrid());
        endDatePicker.addValueChangeListener(e -> updateGrid());

        // Add credit salary button
        Button creditSalaryButton = new Button("Credit Salary", new Icon(VaadinIcon.MONEY));
        creditSalaryButton.addClickListener(e -> openCreditSalaryDialog());

        filterLayout.add(facultyComboBox, startDatePicker, endDatePicker, creditSalaryButton);
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
        salaryGrid.addColumn(salary -> salary.getFaculty().getFirstName() + " " + salary.getFaculty().getLastName())
                .setHeader("Faculty").setSortable(true);
        salaryGrid.addColumn(FacultySalary::getSalaryDate).setHeader("Salary Date").setSortable(true);
        salaryGrid.addColumn(FacultySalary::getAmount).setHeader("Amount").setSortable(true);
        salaryGrid.addColumn(FacultySalary::getPaymentMethod).setHeader("Payment Method").setSortable(true);
        salaryGrid.addColumn(FacultySalary::getStatus).setHeader("Status").setSortable(true);
        salaryGrid.addColumn(FacultySalary::getTransactionReference).setHeader("Transaction Reference");
        salaryGrid.addColumn(FacultySalary::getRemarks).setHeader("Remarks");

        // Add action column
        salaryGrid.addColumn(new ComponentRenderer<>(salary -> {
            HorizontalLayout layout = new HorizontalLayout();
            if (salary.getStatus() == FacultySalary.Status.PENDING) {
                Button markPaidButton = new Button("Mark as Paid", new Icon(VaadinIcon.CHECK));
                markPaidButton.addClickListener(e -> {
                    try {
                        facultySalaryService.updateSalaryStatus(salary.getSalaryId(), FacultySalary.Status.PAID);
                        updateGrid();
                        Notification.show("Salary marked as paid successfully", 3000, Notification.Position.MIDDLE)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    } catch (Exception ex) {
                        String errorMessage = ex.getMessage();
                        if (errorMessage.contains("Salary can only be paid in its respective month")) {
                            errorMessage = "You can only pay salaries in their respective months. " +
                                    "This salary is for " + YearMonth.from(salary.getSalaryDate()) + 
                                    " and current month is " + YearMonth.now();
                        }
                        Notification.show(errorMessage, 3000,
                                Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                layout.add(markPaidButton);
            }
            return layout;
        })).setHeader("Actions");
    }

    private void openCreditSalaryDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Credit Faculty Salary");

        FormLayout formLayout = new FormLayout();

        ComboBox<User> facultySelect = new ComboBox<>("Faculty");
        // Get all faculty members
        List<User> allFaculty = userService.getUsersByRole(User.Role.FACULTY);
        // Get faculty with pending salaries
        List<User> facultyWithPendingSalaries = facultySalaryService.getFacultyWithPendingSalaries();
        // Filter out faculty with pending salaries
        List<User> availableFaculty = allFaculty.stream()
                .filter(faculty -> !facultyWithPendingSalaries.contains(faculty))
                .toList();
        
        facultySelect.setItems(availableFaculty);
        facultySelect.setItemLabelGenerator(user -> user.getFirstName() + " " + user.getLastName());
        facultySelect.setRequired(true);

        if (availableFaculty.isEmpty()) {
            Notification.show("All faculty members already have pending salaries", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            dialog.close();
            return;
        }

        NumberField amountField = new NumberField("Amount");
        amountField.setRequired(true);
        amountField.setMin(0);
        amountField.setStep(0.01);

        DatePicker salaryDatePicker = new DatePicker("Salary Date");
        salaryDatePicker.setRequired(true);
        salaryDatePicker.setValue(LocalDate.now());

        ComboBox<FacultySalary.PaymentMethod> paymentMethodComboBox = new ComboBox<>("Payment Method");
        paymentMethodComboBox.setItems(FacultySalary.PaymentMethod.values());
        paymentMethodComboBox.setRequired(true);

        TextField transactionReferenceField = new TextField("Transaction Reference");
        TextArea remarksField = new TextArea("Remarks");

        formLayout.add(facultySelect, amountField, salaryDatePicker, paymentMethodComboBox,
                transactionReferenceField, remarksField);

        Button saveButton = new Button("Credit Salary", e -> {
            try {
                facultySalaryService.creditSalary(
                        facultySelect.getValue().getUserId(),
                        BigDecimal.valueOf(amountField.getValue()),
                        salaryDatePicker.getValue(),
                        paymentMethodComboBox.getValue(),
                        transactionReferenceField.getValue(),
                        remarksField.getValue()
                );
                dialog.close();
                updateGrid();
                Notification.show("Salary credited successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Error crediting salary: " + ex.getMessage(), 3000,
                        Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(formLayout);
        dialog.open();
    }

    private void updateGrid() {
        List<FacultySalary> salaries;
        if (facultyComboBox.getValue() != null) {
            salaries = facultySalaryService.getFacultySalariesByDateRange(
                    facultyComboBox.getValue(),
                    startDatePicker.getValue(),
                    endDatePicker.getValue()
            );
        } else {
            salaries = facultySalaryService.getPendingSalaries();
        }
        salaryGrid.setItems(salaries);

        // Update summary
        BigDecimal totalAmount = salaries.stream()
                .map(FacultySalary::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalSalaries.setText("Total Salaries: ₹" + totalAmount + 
                            " | Number of Records: " + salaries.size());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!sessionService.isLoggedIn()) {
            event.rerouteTo("login");
        }
    }
} 