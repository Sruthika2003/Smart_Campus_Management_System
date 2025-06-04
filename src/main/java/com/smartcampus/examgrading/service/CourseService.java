package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.Enrollment;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.Timetable;
import com.smartcampus.examgrading.repository.CourseRepository;
import com.smartcampus.examgrading.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAllCoursesWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesWithAvailableSeats() {
        return courseRepository.findCoursesWithAvailableSeats().stream()
                .filter(course -> course.getAvailableSeats() > 0)
                .toList();
    }

    public Optional<Course> getCourseById(Long courseId) {
        return courseRepository.findById(courseId);
    }

    public List<Course> getCoursesByFaculty(User faculty) {
        return courseRepository.findByFaculty(faculty);
    }

    /**
     * Get all courses a student is enrolled in
     * 
     * @param student The student user
     * @return List of Course objects the student is enrolled in
     */
    @Transactional(readOnly = true)
    public List<Course> getEnrolledCourses(User student) {
        List<Enrollment> enrollments = getStudentEnrollments(student);
        return enrollments.stream()
            .map(Enrollment::getCourse)
            .collect(Collectors.toList());
    }

    @Transactional
    public Course updateCourse(Course course) {
        // Fetch the course with schedules to ensure we have the complete entity
        Course existingCourse = courseRepository.findByIdWithSchedules(course.getId())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        // Update the course
        existingCourse.setSchedules(course.getSchedules());
        return courseRepository.save(existingCourse);
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getStudentEnrollments(User student) {
        return enrollmentRepository.findByStudentAndActiveTrue(student);
    }

    public List<Enrollment> getEnrollmentsForCourse(Course course) {
        return enrollmentRepository.findByCourseAndActiveTrue(course);
    }

    @Transactional
    public Enrollment enrollStudentInCourse(User student, Course course) {
        try {
            // First, ensure we have the complete course entity by fetching it fresh from the database
            Course freshCourse = courseRepository.findById(course.getId())
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            // Check if student is already enrolled - include inactive enrollments in check
            Optional<Enrollment> existingEnrollment = enrollmentRepository.findByStudentAndCourse(student, freshCourse);

            if (existingEnrollment.isPresent()) {
                Enrollment enrollment = existingEnrollment.get();
                // If enrollment exists but is inactive, reactivate it instead of creating a new one
                if (!enrollment.isActive()) {
                    enrollment.setActive(true);
                    enrollment.setEnrollmentDate(LocalDateTime.now());
                    return enrollmentRepository.save(enrollment);
                } else {
                    // Already actively enrolled
                    throw new RuntimeException("Student is already enrolled in this course");
                }
            }

            // Check if course has available seats
            int currentEnrollments = enrollmentRepository.countActiveByCourse(freshCourse);
            if (currentEnrollments >= freshCourse.getCapacity()) {
                throw new RuntimeException("Course has reached its enrollment capacity");
            }

            // Create enrollment
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(student);
            enrollment.setCourse(freshCourse);
            enrollment.setEnrollmentDate(LocalDateTime.now());
            enrollment.setActive(true);

            return enrollmentRepository.save(enrollment);
        } catch (Exception e) {
            throw new RuntimeException("Error enrolling in course: " + e.getMessage(), e);
        }
    }

    public boolean canEnrollInCourse(Course course) {
        return course.getAvailableSeats() > 0;
    }

    @Transactional
    public Course addScheduleToCourse(Course course, Timetable schedule) {
        // Fetch the course with schedules to ensure we have the complete entity
        Course existingCourse = courseRepository.findByIdWithSchedules(course.getId())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        schedule.setCourse(existingCourse);
        existingCourse.getSchedules().add(schedule);
        return courseRepository.save(existingCourse);
    }

    @Transactional
    public Course removeScheduleFromCourse(Course course, Timetable scheduleToRemove) {
        // Fetch the course with schedules to ensure we have the complete entity
        Course existingCourse = courseRepository.findByIdWithSchedules(course.getId())
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        existingCourse.getSchedules().removeIf(schedule -> 
            schedule.getDayOfWeek().equals(scheduleToRemove.getDayOfWeek()) &&
            schedule.getStartTime().equals(scheduleToRemove.getStartTime()) &&
            schedule.getEndTime().equals(scheduleToRemove.getEndTime()) &&
            schedule.getLocation().equals(scheduleToRemove.getLocation())
        );
        return courseRepository.save(existingCourse);
    }

    @Transactional(readOnly = true)
    public List<Timetable> getSchedulesForCourse(Course course) {
        return course.getSchedules();
    }

    @Transactional(readOnly = true)
    public List<Course> getAllCoursesWithSchedules() {
        return courseRepository.findAllCoursesWithDetails();
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByFacultyWithSchedules(User faculty) {
        return courseRepository.findByFacultyWithSchedules(faculty);
    }

    @Transactional(readOnly = true)
    public String getCourseDescription(Long courseId) {
        return courseRepository.findById(courseId)
                .map(Course::getContent)
                .orElse("No description available");
    }

    @Transactional
    public void dropCourse(User student, Course course) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findByStudentAndCourse(student, course);
        
        if (enrollmentOpt.isPresent()) {
            Enrollment enrollment = enrollmentOpt.get();
            enrollment.dropCourse();
            enrollmentRepository.save(enrollment);
        } else {
            throw new RuntimeException("Student is not enrolled in this course");
        }
    }

    @Transactional(readOnly = true)
    public Course getCourseWithMaterials(Long courseId) {
        return courseRepository.findCourseWithMaterials(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + courseId));
    }

    @Transactional(readOnly = true)
    public List<Enrollment> getStudentEnrollmentsWithSchedules(User student) {
        return enrollmentRepository.findByStudentWithSchedules(student);
    }

    @Transactional
    public Course saveCourse(Course course) {
        // For new courses
        if (course.getId() == null) {
            // Check if course code already exists
            if (courseRepository.findByCourseCode(course.getCourseCode()).isPresent()) {
                throw new RuntimeException("Course code already exists");
            }
            return courseRepository.save(course);
        }
        
        // For existing courses
        Course existingCourse = courseRepository.findById(course.getId())
            .orElseThrow(() -> new RuntimeException("Course not found"));
        
        // Update fields
        existingCourse.setCourseCode(course.getCourseCode());
        existingCourse.setCourseName(course.getCourseName());
        existingCourse.setContent(course.getContent());
        existingCourse.setCreditHours(course.getCreditHours());
        existingCourse.setCapacity(course.getCapacity());
        existingCourse.setFaculty(course.getFaculty());
        
        return courseRepository.save(existingCourse);
    }
}