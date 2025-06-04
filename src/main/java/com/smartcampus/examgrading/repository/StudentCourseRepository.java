package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.StudentCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCourseRepository extends JpaRepository<StudentCourse, Long> {

    /**
     * Find all courses a student is registered for
     * 
     * @param studentId The ID of the student
     * @return List of StudentCourse entities for the given student
     */
    List<StudentCourse> findByStudentId(Long studentId);

    /**
     * Find all students registered for a course
     * 
     * @param courseId The ID of the course
     * @return List of StudentCourse entities for the given course
     */
    List<StudentCourse> findByCourseId(Long courseId);

    /**
     * Find a specific student-course registration
     * 
     * @param studentId The ID of the student
     * @param courseId  The ID of the course
     * @return Optional StudentCourse entity if found
     */
    Optional<StudentCourse> findByStudentIdAndCourseId(Long studentId, Long courseId);
}