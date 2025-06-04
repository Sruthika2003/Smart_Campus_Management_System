package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.StudentFee;
import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {
    List<StudentFee> findByStudent(User student);
    
    List<StudentFee> findByStudentAndStatus(User student, StudentFee.Status status);
    
    List<StudentFee> findByStudentAndSemesterAndAcademicYear(
            User student, String semester, String academicYear);
            
    @Query("SELECT sf FROM StudentFee sf WHERE sf.student = :student AND sf.dueDate <= :currentDate AND sf.status = 'PENDING'")
    List<StudentFee> findOverdueFees(@Param("student") User student, @Param("currentDate") LocalDate currentDate);

    List<StudentFee> findByStatus(StudentFee.Status status);
    
    List<StudentFee> findByStatusAndSemesterAndAcademicYear(
        StudentFee.Status status,
        String semester,
        String academicYear
    );
    
    List<StudentFee> findByStatusAndAcademicYear(
        StudentFee.Status status,
        String academicYear
    );
    
    List<StudentFee> findByStatusAndSemester(
        StudentFee.Status status,
        String semester
    );
} 