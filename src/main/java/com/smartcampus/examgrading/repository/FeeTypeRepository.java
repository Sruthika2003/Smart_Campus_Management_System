package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeTypeRepository extends JpaRepository<FeeType, Long> {
    List<FeeType> findByFrequency(FeeType.Frequency frequency);
} 