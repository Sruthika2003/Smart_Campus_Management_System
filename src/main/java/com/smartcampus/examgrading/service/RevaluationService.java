package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.Grade;
import com.smartcampus.examgrading.model.RevaluationRequest;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.repository.RevaluationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RevaluationService {
    private final RevaluationRequestRepository revaluationRequestRepository;
    private final GradeService gradeService;
    private final SessionService sessionService;

    public RevaluationService(RevaluationRequestRepository revaluationRequestRepository,
            GradeService gradeService,
            SessionService sessionService) {
        this.revaluationRequestRepository = revaluationRequestRepository;
        this.gradeService = gradeService;
        this.sessionService = sessionService;
    }

    @Transactional
    public RevaluationRequest createRevaluationRequest(Long gradeId, String reason) {
        Grade grade = gradeService.getGradeById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Grade not found"));

        User currentUser = sessionService.getCurrentUser();
        if (!currentUser.getId().equals(grade.getStudentId())) {
            throw new IllegalArgumentException("You can only request revaluation for your own grades");
        }

        RevaluationRequest request = new RevaluationRequest();
        request.setStudent(currentUser);
        request.setExam(grade.getExam());
        request.setGrade(grade);
        request.setReason(reason);
        request.setRequestedAt(LocalDateTime.now());
        request.setStatus(RevaluationRequest.Status.PENDING);

        return revaluationRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<RevaluationRequest> getStudentRevaluationRequests(Long studentId) {
        return revaluationRequestRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<RevaluationRequest> getPendingRevaluationRequestsForCourse(Long courseId) {
        List<RevaluationRequest> requests = revaluationRequestRepository.findByExamCourseId(courseId);
        return requests.stream()
                .filter(request -> request.getStatus() == RevaluationRequest.Status.PENDING)
                .toList();
    }

    @Transactional
    public RevaluationRequest processRevaluationRequest(Long requestId, RevaluationRequest.Status status,
            String notes) {
        RevaluationRequest request = revaluationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Revaluation request not found"));

        User currentUser = sessionService.getCurrentUser();
        request.setProcessedBy(currentUser);
        request.setProcessedAt(LocalDateTime.now());
        request.setStatus(status);
        request.setProcessingNotes(notes);

        return revaluationRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public RevaluationRequest getRevaluationRequest(Long requestId) {
        return revaluationRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Revaluation request not found"));
    }
}