package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.*;
import com.smartcampus.examgrading.repository.FeeTypeRepository;
import com.smartcampus.examgrading.repository.PaymentRepository;
import com.smartcampus.examgrading.repository.StudentFeeRepository;
import com.smartcampus.examgrading.repository.FeeAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeService {
    private final FeeTypeRepository feeTypeRepository;
    private final StudentFeeRepository studentFeeRepository;
    private final PaymentRepository paymentRepository;
    private final SessionService sessionService;
    private final FeeAlertRepository feeAlertRepository;

    public List<FeeType> getAllFeeTypes() {
        return feeTypeRepository.findAll();
    }

    public List<FeeType> getFeeTypesByFrequency(FeeType.Frequency frequency) {
        return feeTypeRepository.findByFrequency(frequency);
    }

    public List<StudentFee> getStudentFees(User student) {
        return studentFeeRepository.findByStudent(student);
    }

    public List<StudentFee> getStudentFeesByStatus(User student, StudentFee.Status status) {
        return studentFeeRepository.findByStudentAndStatus(student, status);
    }

    public List<StudentFee> getStudentFeesBySemester(User student, String semester, String academicYear) {
        return studentFeeRepository.findByStudentAndSemesterAndAcademicYear(student, semester, academicYear);
    }

    public List<StudentFee> getOverdueFees(User student) {
        return studentFeeRepository.findOverdueFees(student, LocalDate.now());
    }

    @Transactional
    public Payment processPayment(StudentFee studentFee, Payment.PaymentMethod paymentMethod, String remarks) {
        // Verify the fee is pending
        if (studentFee.getStatus() != StudentFee.Status.PENDING) {
            throw new RuntimeException("Fee is already paid");
        }

        // Create payment record
        Payment payment = new Payment();
        payment.setStudent(studentFee.getStudent());
        payment.setStudentFee(studentFee);
        payment.setAmount(studentFee.getAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionReference(generateTransactionReference(paymentMethod));
        payment.setReceiptNumber(generateReceiptNumber());
        payment.setRecordedBy(sessionService.getCurrentUser());
        payment.setRemarks(remarks);
        payment.setPaymentDate(java.sql.Timestamp.valueOf(LocalDateTime.now()));

        // Update fee status
        studentFee.setStatus(StudentFee.Status.PAID);

        // Save both records
        studentFeeRepository.save(studentFee);
        return paymentRepository.save(payment);
    }

    private String generateTransactionReference(Payment.PaymentMethod paymentMethod) {
        String prefix;
        switch (paymentMethod) {
            case ONLINE_PAYMENT:
                prefix = "UPI";
                break;
            case CREDIT_CARD:
                prefix = "CC";
                break;
            case DEBIT_CARD:
                prefix = "DC";
                break;
            case BANK_TRANSFER:
                prefix = "BT";
                break;
            case CASH:
                prefix = "CSH";
                break;
            default:
                prefix = "PAY";
        }
        
        return prefix + "-" + 
               LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + 
               "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private String generateReceiptNumber() {
        String receiptNumber;
        do {
            receiptNumber = "RCPT-" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + 
                "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (paymentRepository.existsByReceiptNumber(receiptNumber));
        
        return receiptNumber;
    }

    public BigDecimal calculateTotalPendingFees(User student) {
        return studentFeeRepository.findByStudentAndStatus(student, StudentFee.Status.PENDING)
                .stream()
                .map(StudentFee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalPaidFees(User student) {
        return studentFeeRepository.findByStudentAndStatus(student, StudentFee.Status.PAID)
                .stream()
                .map(StudentFee::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Payment> getStudentPayments(User student) {
        return paymentRepository.findByStudent(student);
    }

    public List<Payment> getStudentPaymentsByAcademicYear(User student, String academicYear) {
        return paymentRepository.findByStudentAndAcademicYear(student, academicYear);
    }

    public List<Payment> getAllPayments(String academicYear, LocalDate startDate, LocalDate endDate) {
        if (academicYear == null || startDate == null || endDate == null) {
            return paymentRepository.findAll();
        }
        
        return paymentRepository.findByPaymentDateBetweenAndStudentFee_AcademicYear(
            startDate.atStartOfDay(),
            endDate.plusDays(1).atStartOfDay(),
            academicYear
        );
    }

    public List<StudentFee> getPendingFees(String academicYear, String semester) {
        if (academicYear == null && semester == null) {
            return studentFeeRepository.findByStatus(StudentFee.Status.PENDING);
        } else if (academicYear != null && semester != null) {
            return studentFeeRepository.findByStatusAndSemesterAndAcademicYear(
                StudentFee.Status.PENDING,
                semester,
                academicYear
            );
        } else if (academicYear != null) {
            return studentFeeRepository.findByStatusAndAcademicYear(
                StudentFee.Status.PENDING,
                academicYear
            );
        } else {
            return studentFeeRepository.findByStatusAndSemester(
                StudentFee.Status.PENDING,
                semester
            );
        }
    }

    public List<FeeAlert> getFeeAlertsForStudent(User student) {
        return feeAlertRepository.findByStudent(student);
    }
} 