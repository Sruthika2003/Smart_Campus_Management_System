package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Attendance;
import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    List<Attendance> findByStudent(User student);
    
    List<Attendance> findByStudentAndCourse(User student, Course course);
    
    List<Attendance> findByCourse(Course course);
    
    List<Attendance> findByCourseAndAttendanceDate(Course course, LocalDate date);
    
    Optional<Attendance> findByStudentAndCourseAndAttendanceDate(User student, Course course, LocalDate date);
    
    @Query("SELECT a FROM Attendance a WHERE a.student = ?1 AND MONTH(a.attendanceDate) = ?2 AND YEAR(a.attendanceDate) = ?3")
    List<Attendance> findByStudentAndMonthAndYear(User student, int month, int year);
    
    @Query("SELECT a FROM Attendance a WHERE a.student = ?1 AND a.course = ?2 AND MONTH(a.attendanceDate) = ?3 AND YEAR(a.attendanceDate) = ?4")
    List<Attendance> findByStudentAndCourseAndMonthAndYear(User student, Course course, int month, int year);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student = ?1 AND a.course = ?2 AND a.status = 'PRESENT'")
    long countPresentByStudentAndCourse(User student, Course course);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student = ?1 AND a.course = ?2")
    long countAllByStudentAndCourse(User student, Course course);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student = ?1 AND a.course = ?2 AND a.status = 'ABSENT'")
    long countAbsentByStudentAndCourse(User student, Course course);
} 