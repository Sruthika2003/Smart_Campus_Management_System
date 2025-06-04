package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.Exam;
import com.smartcampus.examgrading.model.Grade;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.ExamRepository;
import com.smartcampus.examgrading.repository.GradeRepository;
import com.smartcampus.examgrading.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GradeService {

    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final SessionService sessionService;

    public GradeService(GradeRepository gradeRepository, UserRepository userRepository,
            ExamRepository examRepository, SessionService sessionService) {
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.sessionService = sessionService;
    }

    /**
     * Get all grades
     */
    public List<Grade> getAllGrades() {
        return gradeRepository.findAll();
    }

    /**
     * Get all grades for a specific student
     */
    public List<Grade> getGradesByStudentId(Long studentId) {
        List<Grade> grades = gradeRepository.findByStudentId(studentId);

        // Populate related entities
        return grades.stream()
                .map(this::populateGradeRelations)
                .collect(Collectors.toList());
    }

    /**
     * Get all grades for a specific exam
     */
    public List<Grade> getGradesByExamId(Long examId) {
        List<Grade> grades = gradeRepository.findByExamId(examId);

        // Populate related entities
        return grades.stream()
                .map(this::populateGradeRelations)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific grade by student and exam IDs
     */
    public Optional<Grade> getGradeByStudentAndExam(Long studentId, Long examId) {
        Optional<Grade> gradeOpt = gradeRepository.findByStudentIdAndExamId(studentId, examId);

        // Populate related entities if present
        return gradeOpt.map(this::populateGradeRelations);
    }

    /**
     * Get a grade by its ID
     */
    public Optional<Grade> getGradeById(Long gradeId) {
        return gradeRepository.findById(gradeId)
                .map(this::populateGradeRelations);
    }

    /**
     * Save or update a grade
     */
    public Grade saveGrade(Grade grade) {
        // Verify user is authorized (faculty or admin)
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null || (!sessionService.isFaculty())) {
            throw new RuntimeException("Only faculty  can enter grades");
        }

        // Verify student exists
        userRepository.findById(grade.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found with id: " + grade.getStudentId()));

        // Verify exam exists
        Exam exam = examRepository.findById(grade.getExamId())
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + grade.getExamId()));

        // Calculate percentage
        BigDecimal percentage = grade.getMarksObtained()
                .multiply(new BigDecimal(100))
                .divide(exam.getTotalMarks(), 2, RoundingMode.HALF_UP);
        grade.setPercentage(percentage);

        // Determine grade letter based on percentage
        grade.setGradeLetter(calculateGradeLetter(percentage));

        // Set graded by and timestamp
        grade.setGradedBy(currentUser.getUserId());
        grade.setGradedAt(Timestamp.valueOf(LocalDateTime.now()));

        // Save the grade
        return gradeRepository.save(grade);
    }

    /**
     * Delete a grade
     */
    public void deleteGrade(Long gradeId) {
        // Verify user is authorized (faculty or admin)
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null || (!sessionService.isFaculty() && !sessionService.isAdmin())) {
            throw new RuntimeException("Only faculty and administrators can delete grades");
        }

        gradeRepository.deleteById(gradeId);
    }

    /**
     * Calculate grade letter based on percentage
     */
    private String calculateGradeLetter(BigDecimal percentage) {
        int value = percentage.intValue();

        if (value >= 90)
            return "A+";
        if (value >= 85)
            return "A";
        if (value >= 80)
            return "A-";
        if (value >= 75)
            return "B+";
        if (value >= 70)
            return "B";
        if (value >= 65)
            return "B-";
        if (value >= 60)
            return "C+";
        if (value >= 55)
            return "C";
        if (value >= 50)
            return "C-";
        if (value >= 45)
            return "D+";
        if (value >= 40)
            return "D";
        return "F";
    }

    /**
     * Populate grade with related entities
     */
    private Grade populateGradeRelations(Grade grade) {
        userRepository.findById(grade.getStudentId())
                .ifPresent(grade::setStudent);

        examRepository.findById(grade.getExamId())
                .ifPresent(grade::setExam);

        userRepository.findById(grade.getGradedBy())
                .ifPresent(grade::setGrader);

        return grade;
    }
}