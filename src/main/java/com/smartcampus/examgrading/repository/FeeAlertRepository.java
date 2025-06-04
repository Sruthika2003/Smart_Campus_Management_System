package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.FeeAlert;
import com.smartcampus.examgrading.model.StudentFee;
import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeAlertRepository extends JpaRepository<FeeAlert, Long> {
    List<FeeAlert> findByStudentFee(StudentFee studentFee);
    List<FeeAlert> findByStudent(User student);
    List<FeeAlert> findByAccounts(User accounts);
    boolean existsByStudentFeeAndStatus(StudentFee studentFee, FeeAlert.Status status);
} 