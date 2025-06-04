package com.smartcampus.examgrading.repository;

import com.smartcampus.examgrading.model.Attendance;
import com.smartcampus.examgrading.model.AttendanceCorrectionRequest;
import com.smartcampus.examgrading.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceCorrectionRequestRepository extends JpaRepository<AttendanceCorrectionRequest, Long> {
    List<AttendanceCorrectionRequest> findByAttendance(Attendance attendance);
    List<AttendanceCorrectionRequest> findByRequestedBy(User student);
    
    List<AttendanceCorrectionRequest> findByAttendanceAndStatus(Attendance attendance, AttendanceCorrectionRequest.RequestStatus status);
    
    List<AttendanceCorrectionRequest> findByStatus(AttendanceCorrectionRequest.RequestStatus status);
    
    Optional<AttendanceCorrectionRequest> findByAttendanceAndRequestedByAndStatus(
            Attendance attendance, 
            User requestedBy, 
            AttendanceCorrectionRequest.RequestStatus status);
    
    // Find correction requests for attendances marked by a specific faculty
    List<AttendanceCorrectionRequest> findByAttendance_MarkedBy(User faculty);
} 