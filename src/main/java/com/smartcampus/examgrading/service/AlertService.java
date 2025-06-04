package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.FeeAlert;
import com.smartcampus.examgrading.model.StudentFee;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.FeeAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AlertService {
    private final FeeAlertRepository feeAlertRepository;

    public AlertService(FeeAlertRepository feeAlertRepository) {
        this.feeAlertRepository = feeAlertRepository;
    }

    @Transactional
    public FeeAlert generateFeeAlert(StudentFee fee, User accountsUser) {
        try {
            // Check if an alert was already sent for this fee
            if (hasAlertBeenSent(fee)) {
                throw new IllegalStateException("An alert has already been sent for this fee");
            }

            String alertMessage = generateAlertMessage(fee);
            
            FeeAlert alert = new FeeAlert();
            alert.setStudentFee(fee);
            alert.setStudent(fee.getStudent());
            alert.setAccounts(accountsUser);
            alert.setMessage(alertMessage);

            return feeAlertRepository.save(alert);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate fee alert: " + e.getMessage(), e);
        }
    }

    public boolean hasAlertBeenSent(StudentFee fee) {
        try {
            return feeAlertRepository.existsByStudentFeeAndStatus(fee, FeeAlert.Status.SENT);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check alert status: " + e.getMessage(), e);
        }
    }

    public String generateAlertMessage(StudentFee fee) {
        try {
            User student = fee.getStudent();
            LocalDate dueDate = fee.getDueDate();
            long daysOverdue = LocalDate.now().toEpochDay() - dueDate.toEpochDay();
            
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("studentName", student.getFirstName() + " " + student.getLastName());
            templateData.put("studentId", student.getUsername());
            templateData.put("feeType", fee.getFeeType().getFeeName());
            templateData.put("amount", fee.getAmount());
            templateData.put("dueDate", dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            templateData.put("daysOverdue", daysOverdue);
            templateData.put("semester", fee.getSemester());
            templateData.put("academicYear", fee.getAcademicYear());
            
            return formatAlertMessage(templateData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate alert message: " + e.getMessage(), e);
        }
    }

    private String formatAlertMessage(Map<String, Object> data) {
        StringBuilder message = new StringBuilder();
        message.append("Dear ").append(data.get("studentName")).append(",\n\n");
        message.append("This is a reminder regarding your pending fee payment.\n\n");
        message.append("Fee Details:\n");
        message.append("Fee Type: ").append(data.get("feeType")).append("\n");
        message.append("Amount: ₹").append(data.get("amount")).append("\n");
        message.append("Due Date: ").append(data.get("dueDate")).append("\n");
        
        long daysOverdue = (long) data.get("daysOverdue");
        if (daysOverdue > 0) {
            message.append("Status: OVERDUE by ").append(daysOverdue).append(" days\n");
        } else {
            message.append("Status: PENDING\n");
        }
        
        message.append("\nPlease make the payment at the earliest to avoid any inconvenience.\n");
        message.append("You can make the payment through the Smart Campus portal.\n\n");
        message.append("Best regards,\n");
        message.append("Accounts Department\n");
        message.append("Smart Campus");
        
        return message.toString();
    }
} 