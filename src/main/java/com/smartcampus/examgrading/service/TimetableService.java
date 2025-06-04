package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Timetable;
import com.smartcampus.examgrading.repository.CourseRepository;
import com.smartcampus.examgrading.repository.TimetableRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final CourseRepository courseRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Timetable> getSchedulesForCourse(Long courseId) {
        return timetableRepository.findByCourseIdWithCourse(courseId);
    }

    @Transactional
    public Timetable saveSchedule(Timetable schedule) {
        if (schedule.getCourse() == null || schedule.getCourse().getId() == null) {
            throw new IllegalArgumentException("Course information is required");
        }

        Course course = courseRepository.findById(schedule.getCourse().getId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        // Initialize the schedules collection if it's null
        if (course.getSchedules() == null) {
            course.setSchedules(new ArrayList<>());
        }

        // Set up bidirectional relationship
        schedule.setCourse(course);
        course.addSchedule(schedule);

        // Save the course which will cascade to the schedule
        course = courseRepository.save(course);

        // Return the saved schedule from the course's schedule list
        return course.getSchedules().get(course.getSchedules().size() - 1);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        Timetable schedule = timetableRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
        
        Course course = schedule.getCourse();
        course.removeSchedule(schedule);
        courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public List<Timetable> getAllSchedules() {
        return timetableRepository.findAllWithCourse();
    }

    @Transactional(readOnly = true)
    public Timetable getScheduleById(Long id) {
        return timetableRepository.findByIdWithCourse(id)
                .orElseThrow(() -> new EntityNotFoundException("Schedule not found"));
    }
} 