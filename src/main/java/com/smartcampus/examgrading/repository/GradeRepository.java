package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudentId(Long studentId);

    List<Grade> findByExamId(Long examId);

    Optional<Grade> findByStudentIdAndExamId(Long studentId, Long examId);
}
