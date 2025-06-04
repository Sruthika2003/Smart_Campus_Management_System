package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.FacultySalary;
import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FacultySalaryRepository extends JpaRepository<FacultySalary, Long> {
    List<FacultySalary> findByFaculty(User faculty);
    List<FacultySalary> findByFacultyAndSalaryDateBetween(User faculty, LocalDate startDate, LocalDate endDate);
    List<FacultySalary> findByStatus(FacultySalary.Status status);
    List<FacultySalary> findByCreditedBy(User creditedBy);
    List<FacultySalary> findByFacultyAndSalaryDateBetweenAndStatus(User faculty, LocalDate startDate, LocalDate endDate, FacultySalary.Status status);
} 