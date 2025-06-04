package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.StudentCourse;
import com.smartcampus.examgrading.repository.CourseRepository;
import com.smartcampus.examgrading.repository.StudentCourseRepository;
import com.smartcampus.examgrading.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentCourseRepository studentCourseRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public StudentService(StudentCourseRepository studentCourseRepository,
            CourseRepository courseRepository,
            UserRepository userRepository) {
        this.studentCourseRepository = studentCourseRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get all courses a student is registered for
     * 
     * @param studentId The ID of the student
     * @return List of courses the student is registered for
     */
    public List<Course> getStudentCourses(Long studentId) {
        // Verify student exists and is a student
        Optional<User> studentOpt = userRepository.findById(studentId);
        if (studentOpt.isEmpty() || studentOpt.get().getRole() != User.Role.STUDENT) {
            return new ArrayList<>();
        }

        // Get all course registrations for this student
        List<StudentCourse> registrations = studentCourseRepository.findByStudentId(studentId);

        // Get the course details for each registration
        return registrations.stream()
                .map(reg -> courseRepository.findById(reg.getCourseId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Register a student for a course
     * 
     * @param studentId The ID of the student
     * @param courseId  The ID of the course
     * @return true if registration was successful, false otherwise
     */
    public boolean registerStudentForCourse(Long studentId, Long courseId) {
        // Verify student exists and is a student
        Optional<User> studentOpt = userRepository.findById(studentId);
        if (studentOpt.isEmpty() || studentOpt.get().getRole() != User.Role.STUDENT) {
            return false;
        }

        // Verify course exists
        if (courseRepository.findById(courseId).isEmpty()) {
            return false;
        }

        // Check if already registered
        if (studentCourseRepository.findByStudentIdAndCourseId(studentId, courseId).isPresent()) {
            return true; // Already registered
        }

        // Create new registration
        StudentCourse registration = new StudentCourse();
        registration.setStudentId(studentId);
        registration.setCourseId(courseId);
        studentCourseRepository.save(registration);

        return true;
    }

    /**
     * Unregister a student from a course
     * 
     * @param studentId The ID of the student
     * @param courseId  The ID of the course
     * @return true if unregistration was successful, false otherwise
     */
    public boolean unregisterStudentFromCourse(Long studentId, Long courseId) {
        Optional<StudentCourse> registrationOpt = studentCourseRepository.findByStudentIdAndCourseId(studentId,
                courseId);

        if (registrationOpt.isPresent()) {
            studentCourseRepository.delete(registrationOpt.get());
            return true;
        }

        return false;
    }

    public List<User> getStudentsEnrolledInCourse(Long courseId) {
        // Get all registrations for this course
        List<StudentCourse> registrations = studentCourseRepository.findByCourseId(courseId);

        // Extract student IDs from the registrations
        List<Long> studentIds = registrations.stream()
                .map(StudentCourse::getStudentId)
                .collect(Collectors.toList());

        // Fetch students by ID and ensure they have the STUDENT role
        return userRepository.findAllById(studentIds).stream()
                .filter(user -> user.getRole() == User.Role.STUDENT)
                .collect(Collectors.toList());
    }
}