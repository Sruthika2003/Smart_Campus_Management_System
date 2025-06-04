package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.CourseMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {
    List<CourseMaterial> findByCourse(Course course);
}