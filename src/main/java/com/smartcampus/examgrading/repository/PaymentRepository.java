package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Payment;
import com.smartcampus.examgrading.model.StudentFee;
import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStudent(User student);
    
    List<Payment> findByStudentFee(StudentFee studentFee);
    
    boolean existsByReceiptNumber(String receiptNumber);

    @Query("SELECT p FROM Payment p WHERE p.student = :student AND p.studentFee.academicYear = :academicYear")
    List<Payment> findByStudentAndAcademicYear(@Param("student") User student, @Param("academicYear") String academicYear);

    List<Payment> findByPaymentDateBetweenAndStudentFee_AcademicYear(
        LocalDateTime startDate,
        LocalDateTime endDate,
        String academicYear
    );
} 