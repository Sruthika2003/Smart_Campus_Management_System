package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.RevaluationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevaluationRequestRepository extends JpaRepository<RevaluationRequest, Long> {
    List<RevaluationRequest> findByStudentId(Long studentId);

    List<RevaluationRequest> findByExamId(Long examId);

    List<RevaluationRequest> findByStatus(RevaluationRequest.Status status);

    @Query("SELECT rr FROM RevaluationRequest rr JOIN rr.exam e WHERE e.courseId = :courseId")
    List<RevaluationRequest> findByExamCourseId(@Param("courseId") Long courseId);
}