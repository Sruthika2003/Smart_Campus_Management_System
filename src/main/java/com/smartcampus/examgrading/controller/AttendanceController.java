package com.smartcampus.examgrading.controller;

import com.smartcampus.examgrading.model.Attendance;
import com.smartcampus.examgrading.model.AttendanceCorrectionRequest;
import com.smartcampus.examgrading.model.AttendanceReport;
import com.smartcampus.examgrading.model.Course;
import com.smartcampus.examgrading.model.User;
import com.smartcampus.examgrading.service.AttendanceService;
import com.smartcampus.examgrading.service.CourseService;
import com.smartcampus.examgrading.service.SessionService;
import com.smartcampus.examgrading.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final CourseService courseService;
    private final UserService userService;
    private final SessionService sessionService;

    public AttendanceController(
            AttendanceService attendanceService,
            CourseService courseService,
            UserService userService,
            SessionService sessionService) {
        this.attendanceService = attendanceService;
        this.courseService = courseService;
        this.userService = userService;
        this.sessionService = sessionService;
    }

    // Faculty endpoints for marking attendance
    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> request) {
        try {
            if (!sessionService.isFaculty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            Long courseId = Long.valueOf(request.get("courseId").toString());
            Long studentId = Long.valueOf(request.get("studentId").toString());
            LocalDate date = LocalDate.parse(request.get("date").toString());
            String status = request.get("status").toString();

            Optional<Course> courseOpt = courseService.getCourseById(courseId);
            if (!courseOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found");
            }
            
            Optional<User> studentOpt = userService.getUserById(studentId);
            if (!studentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Student not found");
            }
            
            User faculty = sessionService.getCurrentUser();

            Attendance.AttendanceStatus attendanceStatus = Attendance.AttendanceStatus.valueOf(status);
            attendanceService.markAttendance(courseOpt.get(), studentOpt.get(), date, attendanceStatus, faculty);

            return ResponseEntity.ok("Attendance marked successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error marking attendance: " + e.getMessage());
        }
    }

    @PostMapping("/mark-bulk")
    public ResponseEntity<?> markBulkAttendance(@RequestBody Map<String, Object> request) {
        try {
            if (!sessionService.isFaculty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            Long courseId = Long.valueOf(request.get("courseId").toString());
            LocalDate date = LocalDate.parse(request.get("date").toString());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendanceData = (List<Map<String, Object>>) request.get("attendanceData");

            Optional<Course> courseOpt = courseService.getCourseById(courseId);
            if (!courseOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found");
            }
            
            User faculty = sessionService.getCurrentUser();

            // Process bulk attendance data
            for (Map<String, Object> data : attendanceData) {
                Long studentId = Long.valueOf(data.get("studentId").toString());
                String status = data.get("status").toString();
                
                Optional<User> studentOpt = userService.getUserById(studentId);
                if (!studentOpt.isPresent()) {
                    continue; // Skip this student if not found
                }
                
                Attendance.AttendanceStatus attendanceStatus = Attendance.AttendanceStatus.valueOf(status);
                attendanceService.markAttendance(courseOpt.get(), studentOpt.get(), date, attendanceStatus, faculty);
            }

            return ResponseEntity.ok("Bulk attendance marked successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error marking bulk attendance: " + e.getMessage());
        }
    }

    // Student endpoints for viewing attendance
    @GetMapping("/student/{courseId}")
    public ResponseEntity<?> getStudentCourseAttendance(@PathVariable Long courseId) {
        try {
            if (!sessionService.isStudent()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            User student = sessionService.getCurrentUser();
            Optional<Course> courseOpt = courseService.getCourseById(courseId);
            if (!courseOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found");
            }
            
            Course course = courseOpt.get();
            List<Attendance> attendanceList = attendanceService.getStudentCourseAttendance(student, course);
            Map<String, Object> response = new HashMap<>();
            response.put("attendance", attendanceList);
            response.put("percentage", attendanceService.calculateAttendancePercentage(student, course));
            response.put("lowAttendance", attendanceService.hasLowAttendance(student, course));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error retrieving attendance: " + e.getMessage());
        }
    }

    // Endpoints for attendance correction requests
    @PostMapping("/correction-request")
    public ResponseEntity<?> submitCorrectionRequest(@RequestBody Map<String, Object> request) {
        try {
            if (!sessionService.isStudent()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            Long attendanceId = Long.valueOf(request.get("attendanceId").toString());
            String reason = request.get("reason").toString();

            Optional<Attendance> attendanceOpt = attendanceService.findAttendanceById(attendanceId);
            if (!attendanceOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Attendance record not found");
            }
            
            User student = sessionService.getCurrentUser();

            AttendanceCorrectionRequest correctionRequest = 
                attendanceService.submitCorrectionRequest(attendanceOpt.get(), student, reason);

            return ResponseEntity.ok(correctionRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error submitting correction request: " + e.getMessage());
        }
    }

    @GetMapping("/correction-requests")
    public ResponseEntity<?> getCorrectionsForReview() {
        try {
            if (!sessionService.isFaculty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            User faculty = sessionService.getCurrentUser();
            List<AttendanceCorrectionRequest> requests = 
                attendanceService.getPendingRequestsForFaculty(faculty);

            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error retrieving correction requests: " + e.getMessage());
        }
    }

    @PostMapping("/review-correction")
    public ResponseEntity<?> reviewCorrectionRequest(@RequestBody Map<String, Object> request) {
        try {
            if (!sessionService.isFaculty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            Long requestId = Long.valueOf(request.get("requestId").toString());
            String status = request.get("status").toString();
            String comments = request.get("comments").toString();

            User faculty = sessionService.getCurrentUser();
            AttendanceCorrectionRequest.RequestStatus requestStatus = 
                AttendanceCorrectionRequest.RequestStatus.valueOf(status);

            AttendanceCorrectionRequest updatedRequest = 
                attendanceService.reviewCorrectionRequest(requestId, requestStatus, faculty, comments);

            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error reviewing correction request: " + e.getMessage());
        }
    }

    // Admin endpoints for reports
    @GetMapping("/reports/{courseId}/{month}/{year}")
    public ResponseEntity<?> getCourseAttendanceReports(
            @PathVariable Long courseId,
            @PathVariable int month,
            @PathVariable int year) {
        try {
            if (!sessionService.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            Optional<Course> courseOpt = courseService.getCourseById(courseId);
            if (!courseOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found");
            }
            
            List<AttendanceReport> reports = 
                attendanceService.getCourseAttendanceReports(courseOpt.get(), month, year);

            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error retrieving attendance reports: " + e.getMessage());
        }
    }

    @GetMapping("/reports/{month}/{year}")
    public ResponseEntity<?> getAllAttendanceReports(
            @PathVariable int month,
            @PathVariable int year) {
        try {
            if (!sessionService.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            List<AttendanceReport> reports = 
                attendanceService.getAllStudentsAttendanceReports(month, year);

            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error retrieving attendance reports: " + e.getMessage());
        }
    }

    // Generate monthly reports (can be called by admin or automated task)
    @PostMapping("/generate-reports/{month}/{year}")
    public ResponseEntity<?> generateMonthlyReports(
            @PathVariable int month,
            @PathVariable int year) {
        try {
            if (!sessionService.isAdmin() && !sessionService.isFaculty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            attendanceService.generateMonthlyReports(month, year);
            return ResponseEntity.ok("Monthly reports generated successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error generating monthly reports: " + e.getMessage());
        }
    }
} 