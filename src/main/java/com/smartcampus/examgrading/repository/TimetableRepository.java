package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    
    @Query("SELECT t FROM Timetable t LEFT JOIN FETCH t.course WHERE t.course.id = :courseId")
    List<Timetable> findByCourseIdWithCourse(@Param("courseId") Long courseId);
    
    @Query("SELECT t FROM Timetable t LEFT JOIN FETCH t.course")
    List<Timetable> findAllWithCourse();
    
    @Query("SELECT t FROM Timetable t LEFT JOIN FETCH t.course WHERE t.timetableId = :id")
    Optional<Timetable> findByIdWithCourse(@Param("id") Long id);
} 