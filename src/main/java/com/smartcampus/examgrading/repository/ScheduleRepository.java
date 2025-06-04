package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Schedule;
import com.smartcampus.examgrading.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByCourse(Course course);
}