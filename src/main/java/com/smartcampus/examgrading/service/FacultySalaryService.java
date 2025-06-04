package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.FacultySalary;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.FacultySalaryRepository;
import com.smartcampus.examgrading.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class FacultySalaryService {

    @Autowired
    private FacultySalaryRepository facultySalaryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionService sessionService;

    @Transactional
    public FacultySalary creditSalary(Long facultyId, BigDecimal amount, LocalDate salaryDate,
                                    FacultySalary.PaymentMethod paymentMethod, String transactionReference,
                                    String remarks) {
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        if (faculty.getRole() != User.Role.FACULTY) {
            throw new RuntimeException("User is not a faculty member");
        }

        User creditedBy = sessionService.getCurrentUser();
        if (creditedBy == null || creditedBy.getRole() != User.Role.ACCOUNTS) {
            throw new RuntimeException("Only accounts staff can credit salaries");
        }

        // Check if salary already exists for this month
        YearMonth salaryMonth = YearMonth.from(salaryDate);
        Optional<FacultySalary> existingSalary = facultySalaryRepository.findByFacultyAndSalaryDateBetween(
                faculty,
                salaryMonth.atDay(1),
                salaryMonth.atEndOfMonth()
        ).stream().findFirst();

        if (existingSalary.isPresent()) {
            throw new RuntimeException("Salary already exists for " + salaryMonth);
        }

        FacultySalary salary = new FacultySalary();
        salary.setFaculty(faculty);
        salary.setAmount(amount);
        salary.setSalaryDate(salaryDate);
        salary.setPaymentMethod(paymentMethod);
        salary.setTransactionReference(transactionReference);
        salary.setStatus(FacultySalary.Status.PENDING);
        salary.setCreditedBy(creditedBy);
        salary.setCreditedAt(LocalDateTime.now());
        salary.setRemarks(remarks);

        return facultySalaryRepository.save(salary);
    }

    @Transactional
    public FacultySalary updateSalaryStatus(Long salaryId, FacultySalary.Status status) {
        FacultySalary salary = facultySalaryRepository.findById(salaryId)
                .orElseThrow(() -> new RuntimeException("Salary record not found"));

        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null || currentUser.getRole() != User.Role.ACCOUNTS) {
            throw new RuntimeException("Only accounts staff can update salary status");
        }

        // Check if the salary is being marked as paid
        if (status == FacultySalary.Status.PAID) {
            // Get current month
            YearMonth currentMonth = YearMonth.now();
            YearMonth salaryMonth = YearMonth.from(salary.getSalaryDate());

            // Check if the salary is for the current month
            if (!currentMonth.equals(salaryMonth)) {
                throw new RuntimeException("Salary can only be paid in its respective month. " +
                        "This salary is for " + salaryMonth + " and current month is " + currentMonth);
            }
        }

        salary.setStatus(status);
        FacultySalary updatedSalary = facultySalaryRepository.save(salary);

        // If marking as paid, create next month's salary
        if (status == FacultySalary.Status.PAID) {
            createNextMonthSalary(salary);
        }

        return updatedSalary;
    }

    private void createNextMonthSalary(FacultySalary currentSalary) {
        YearMonth nextMonth = YearMonth.from(currentSalary.getSalaryDate()).plusMonths(1);
        LocalDate nextMonthDate = nextMonth.atDay(1);

        // Check if next month's salary already exists
        Optional<FacultySalary> existingNextMonthSalary = facultySalaryRepository.findByFacultyAndSalaryDateBetween(
                currentSalary.getFaculty(),
                nextMonth.atDay(1),
                nextMonth.atEndOfMonth()
        ).stream().findFirst();

        if (existingNextMonthSalary.isEmpty()) {
            FacultySalary nextMonthSalary = new FacultySalary();
            nextMonthSalary.setFaculty(currentSalary.getFaculty());
            nextMonthSalary.setAmount(currentSalary.getAmount()); // Use same amount as current salary
            nextMonthSalary.setSalaryDate(nextMonthDate);
            nextMonthSalary.setPaymentMethod(currentSalary.getPaymentMethod());
            nextMonthSalary.setStatus(FacultySalary.Status.PENDING);
            nextMonthSalary.setCreditedBy(currentSalary.getCreditedBy());
            nextMonthSalary.setCreditedAt(LocalDateTime.now());
            nextMonthSalary.setRemarks("Auto-generated for " + nextMonth);

            facultySalaryRepository.save(nextMonthSalary);
        }
    }

    public List<FacultySalary> getFacultySalaries(User faculty) {
        return facultySalaryRepository.findByFaculty(faculty);
    }

    public List<FacultySalary> getFacultySalariesByDateRange(User faculty, LocalDate startDate, LocalDate endDate) {
        return facultySalaryRepository.findByFacultyAndSalaryDateBetween(faculty, startDate, endDate);
    }

    public List<FacultySalary> getPendingSalaries() {
        return facultySalaryRepository.findByStatus(FacultySalary.Status.PENDING);
    }

    public List<FacultySalary> getSalariesByCreditedBy(User creditedBy) {
        return facultySalaryRepository.findByCreditedBy(creditedBy);
    }

    public List<User> getFacultyWithPendingSalaries() {
        List<FacultySalary> pendingSalaries = facultySalaryRepository.findByStatus(FacultySalary.Status.PENDING);
        return pendingSalaries.stream()
                .map(FacultySalary::getFaculty)
                .distinct()
                .toList();
    }
} 