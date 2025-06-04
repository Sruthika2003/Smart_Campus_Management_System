package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    List<Course> findByFaculty(User faculty);

    @Query("SELECT c FROM Course c WHERE c.faculty.userId = :facultyId")
    List<Course> findByFacultyId(@Param("facultyId") Long facultyId);

    @Query("SELECT c FROM Course c LEFT JOIN FETCH c.faculty LEFT JOIN FETCH c.enrollments")
    List<Course> findCoursesWithAvailableSeats();

    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.schedules")
    List<Course> findAllWithSchedules();

    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.schedules WHERE c.faculty = ?1")
    List<Course> findByFacultyWithSchedules(User faculty);

    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.materials WHERE c.id = ?1")
    Optional<Course> findCourseWithMaterials(Long courseId);

    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.faculty LEFT JOIN FETCH c.enrollments")
    List<Course> findAllCoursesWithDetails();

    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.schedules WHERE c.id = :courseId")
    Optional<Course> findByIdWithSchedules(@Param("courseId") Long courseId);
}
