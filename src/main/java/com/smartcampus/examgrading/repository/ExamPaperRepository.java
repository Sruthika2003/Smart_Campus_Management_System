package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.ExamPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExamPaperRepository extends JpaRepository<ExamPaper, Long> {

    List<ExamPaper> findByExamId(Long examId);

    @Query("SELECT ep FROM ExamPaper ep WHERE ep.examId = :examId AND ep.uploadedBy = :facultyId")
    List<ExamPaper> findByExamIdAndFacultyId(@Param("examId") Long examId, @Param("facultyId") Long facultyId);
}