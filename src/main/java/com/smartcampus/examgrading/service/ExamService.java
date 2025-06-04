package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.Exam;
import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.model.ExamPaper;
import com.smartcampus.examgrading.repository.ExamRepository;
import com.smartcampus.examgrading.repository.CourseRepository;
import com.smartcampus.examgrading.repository.UserRepository;
import com.smartcampus.examgrading.repository.ExamPaperRepository;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExamService {

    private final ExamRepository repository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ExamPaperRepository examPaperRepository;
    private final SessionService sessionService;

    @Value("${exam.papers.upload.dir:./uploads/exam-papers}")
    private String uploadDir;

    public ExamService(ExamRepository repository, CourseRepository courseRepository,
            UserRepository userRepository, ExamPaperRepository examPaperRepository,
            SessionService sessionService) {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.examPaperRepository = examPaperRepository;
        this.sessionService = sessionService;
    }

    @PostConstruct
    public void init() {
        createUploadDirectoryIfNotExists();
    }

    private void createUploadDirectoryIfNotExists() {
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    public List<Exam> getAllExams() {
        return repository.findAll();
    }

    public Optional<Exam> getExamById(Long id) {
        return repository.findById(id);
    }

    public Exam saveExam(Exam exam) {
        // Check if user is authorized to create/edit exams
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null || (!sessionService.isFaculty() && !sessionService.isAdmin())) {
            throw new RuntimeException("Only faculty and administrators can schedule exams");
        }

        return repository.save(exam);
    }

    public void deleteExam(Long id) {
        // Check if user is authorized to delete exams
        User currentUser = sessionService.getCurrentUser();
        if (currentUser == null || (!sessionService.isFaculty() && !sessionService.isAdmin())) {
            throw new RuntimeException("Only faculty and administrators can delete exams");
        }

        repository.deleteById(id);
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Optional<Course> getCourseById(Long id) {
        return courseRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // New methods needed for FacultyGradeView and StudentGradeView
    public List<Course> getCoursesByFacultyId(Long facultyId) {
        // Return courses taught by a specific faculty member
        return courseRepository.findByFacultyId(facultyId);
    }

    public List<Exam> getExamsByCourseId(Long courseId) {
        // Return all exams for a specific course
        return repository.findByCourseId(courseId);
    }

    // New methods for exam paper functionality
    public ExamPaper uploadExamPaper(Long examId, Long facultyId, InputStream fileStream, String originalFilename)
            throws IOException {
        // Validate that exam exists
        Exam exam = repository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId));

        // Validate that user exists and is faculty
        User faculty = userRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + facultyId));

        if (faculty.getRole() != User.Role.FACULTY) {
            throw new RuntimeException("Only faculty can upload exam papers");
        }

        // Generate a unique filename to prevent overwrites
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Create directory for this exam if it doesn't exist
        String examUploadPath = uploadDir + "/" + examId;
        File examDir = new File(examUploadPath);
        if (!examDir.exists()) {
            examDir.mkdirs();
        }

        // Save the file to the filesystem
        Path filePath = Paths.get(examUploadPath, uniqueFilename);
        Files.copy(fileStream, filePath);

        // Create and save the ExamPaper entity
        ExamPaper examPaper = new ExamPaper();
        examPaper.setExamId(examId);
        examPaper.setFileName(originalFilename);
        examPaper.setFilePath(filePath.toString());
        examPaper.setUploadDate(LocalDateTime.now());
        examPaper.setUploadedBy(facultyId);

        return examPaperRepository.save(examPaper);
    }

    public List<ExamPaper> getExamPapersByExamId(Long examId) {
        return examPaperRepository.findByExamId(examId);
    }

    public List<ExamPaper> getExamPapersByExamIdAndFacultyId(Long examId, Long facultyId) {
        return examPaperRepository.findByExamIdAndFacultyId(examId, facultyId);
    }

    public boolean deleteExamPaper(Long paperId, Long facultyId) {
        Optional<ExamPaper> paperOpt = examPaperRepository.findById(paperId);

        if (paperOpt.isPresent()) {
            ExamPaper paper = paperOpt.get();

            // Check if the faculty is the one who uploaded it
            if (!paper.getUploadedBy().equals(facultyId)) {
                throw new RuntimeException("You don't have permission to delete this exam paper");
            }

            // Delete the file from filesystem
            try {
                Files.deleteIfExists(Paths.get(paper.getFilePath()));
                examPaperRepository.delete(paper);
                return true;
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete exam paper file", e);
            }
        }

        return false;
    }
}