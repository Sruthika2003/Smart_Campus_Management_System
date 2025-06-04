package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.AttendanceReport;
import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceReportRepository extends JpaRepository<AttendanceReport, Long> {
    
    List<AttendanceReport> findByStudent(User student);
    
    List<AttendanceReport> findByStudentAndCourse(User student, Course course);
    
    List<AttendanceReport> findByCourse(Course course);
    
    Optional<AttendanceReport> findByStudentAndCourseAndMonthAndYear(User student, Course course, int month, int year);
    
    @Query("SELECT r FROM AttendanceReport r WHERE r.student = ?1 AND r.attendancePercentage < ?2")
    List<AttendanceReport> findLowAttendanceReports(User student, BigDecimal threshold);
    
    @Query("SELECT r FROM AttendanceReport r WHERE r.course = ?1 AND r.month = ?2 AND r.year = ?3")
    List<AttendanceReport> findByCourseAndMonthAndYear(Course course, int month, int year);
} 