package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.MaterialFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaterialFileRepository extends JpaRepository<MaterialFile, Long> {
    Optional<MaterialFile> findByMaterialId(Long materialId);

    void deleteByMaterialId(Long materialId);
}