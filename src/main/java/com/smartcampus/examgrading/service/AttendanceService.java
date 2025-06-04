package com.smartcampus.examgrading.service;

import com.smartcampus.examgrading.model.*;
import com.smartcampus.examgrading.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final BigDecimal LOW_ATTENDANCE_THRESHOLD = new BigDecimal("75.00");

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private AttendanceReportRepository reportRepository;

    @Autowired
    private AttendanceCorrectionRequestRepository correctionRequestRepository;

    @Autowired
    private StudentCourseRepository studentCourseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;

    // Find attendance by ID
    public Optional<Attendance> findAttendanceById(Long attendanceId) {
        return attendanceRepository.findById(attendanceId);
    }
    
    // Find attendance by student, course and date
    public Optional<Attendance> findAttendanceByStudentAndCourseAndDate(User student, Course course, LocalDate date) {
        return attendanceRepository.findByStudentAndCourseAndAttendanceDate(student, course, date);
    }

    // Faculty methods for marking attendance
    @Transactional
    public void markAttendance(Course course, User student, LocalDate date, 
                              Attendance.AttendanceStatus status, User faculty) {
        Optional<Attendance> existingAttendance = 
            attendanceRepository.findByStudentAndCourseAndAttendanceDate(student, course, date);
        
        Attendance attendance;
        if (existingAttendance.isPresent()) {
            attendance = existingAttendance.get();
            attendance.setStatus(status);
            attendance.setMarkedBy(faculty);
            attendance.setMarkedAt(LocalDateTime.now());
        } else {
            attendance = new Attendance();
            attendance.setCourse(course);
            attendance.setStudent(student);
            attendance.setAttendanceDate(date);
            attendance.setStatus(status);
            attendance.setMarkedBy(faculty);
            attendance.setMarkedAt(LocalDateTime.now());
        }
        
        Attendance savedAttendance = attendanceRepository.save(attendance);
        
        // Send notification to admin dashboard
        sendAttendanceAlert(savedAttendance);
    }

    @Transactional
    public void markBulkAttendance(Course course, List<User> students, LocalDate date, 
                                  List<Attendance.AttendanceStatus> statuses, User faculty) {
        List<Attendance> savedAttendances = new ArrayList<>();
        
        for (int i = 0; i < students.size(); i++) {
            Optional<Attendance> existingAttendance = 
                attendanceRepository.findByStudentAndCourseAndAttendanceDate(students.get(i), course, date);
            
            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
                attendance.setStatus(statuses.get(i));
                attendance.setMarkedBy(faculty);
                attendance.setMarkedAt(LocalDateTime.now());
            } else {
                attendance = new Attendance();
                attendance.setCourse(course);
                attendance.setStudent(students.get(i));
                attendance.setAttendanceDate(date);
                attendance.setStatus(statuses.get(i));
                attendance.setMarkedBy(faculty);
                attendance.setMarkedAt(LocalDateTime.now());
            }
            
            savedAttendances.add(attendanceRepository.save(attendance));
        }
        
        // Send batch notification to admin dashboard
        for (Attendance attendance : savedAttendances) {
            sendAttendanceAlert(attendance);
        }
    }
    
    /**
     * Send an alert about attendance changes to the admin dashboard
     * 
     * @param attendance The attendance record that was updated
     */
    private void sendAttendanceAlert(Attendance attendance) {
        // In a real application, this would use a message broker or event system
        // For this implementation, we'll log the event which can be displayed in admin views
        
        String studentName = attendance.getStudent().getFirstName() + " " + attendance.getStudent().getLastName();
        String courseName = attendance.getCourse().getCourseName();
        String facultyName = attendance.getMarkedBy().getFirstName() + " " + attendance.getMarkedBy().getLastName();
        
        // Log this event (in a real app would be stored in database)
        System.out.println("ATTENDANCE_ALERT: " + 
            "Student: " + studentName + 
            ", Course: " + courseName + 
            ", Date: " + attendance.getAttendanceDate() + 
            ", Status: " + attendance.getStatus() + 
            ", Marked by: " + facultyName + 
            ", At: " + attendance.getMarkedAt());
            
        // Additional logic could be added here to:
        // 1. Store in a notifications table
        // 2. Send email alerts to administrators
        // 3. Update real-time dashboard indicators
    }

    // Student methods for viewing attendance
    public List<Attendance> getStudentAttendance(User student) {
        return attendanceRepository.findByStudent(student);
    }

    public List<Attendance> getStudentCourseAttendance(User student, Course course) {
        return attendanceRepository.findByStudentAndCourse(student, course);
    }

    // Calculate attendance percentage for a student in a course
    public BigDecimal calculateAttendancePercentage(User student, Course course) {
        long totalClasses = attendanceRepository.countAllByStudentAndCourse(student, course);
        if (totalClasses == 0) {
            return BigDecimal.ZERO;
        }
        
        long presentClasses = attendanceRepository.countPresentByStudentAndCourse(student, course);
        return BigDecimal.valueOf(presentClasses * 100.0 / totalClasses)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Check if student has low attendance
    public boolean hasLowAttendance(User student, Course course) {
        BigDecimal attendancePercentage = calculateAttendancePercentage(student, course);
        return attendancePercentage.compareTo(LOW_ATTENDANCE_THRESHOLD) < 0;
    }

    // Generate monthly attendance reports
    @Transactional
    public void generateMonthlyReports(int month, int year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // Get all student-course enrollments
        List<StudentCourse> enrollments = studentCourseRepository.findAll();
        
        for (StudentCourse enrollment : enrollments) {
            // Get student and course from IDs in enrollment
            User student = userRepository.findById(enrollment.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            // Get attendance records for the month
            List<Attendance> attendanceRecords = attendanceRepository
                .findByStudentAndCourseAndMonthAndYear(student, course, month, year);
            
            // Calculate statistics
            int totalClasses = attendanceRecords.size();
            int presentCount = (int) attendanceRecords.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
                .count();
            int absentCount = (int) attendanceRecords.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT)
                .count();
            int lateCount = (int) attendanceRecords.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.LATE)
                .count();
            int excusedCount = (int) attendanceRecords.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.EXCUSED)
                .count();
            
            // Calculate percentage
            BigDecimal percentage = BigDecimal.ZERO;
            if (totalClasses > 0) {
                percentage = BigDecimal.valueOf((presentCount + lateCount) * 100.0 / totalClasses)
                    .setScale(2, RoundingMode.HALF_UP);
            }
            
            // Create or update report
            Optional<AttendanceReport> existingReport = 
                reportRepository.findByStudentAndCourseAndMonthAndYear(student, course, month, year);
            
            AttendanceReport report;
            if (existingReport.isPresent()) {
                report = existingReport.get();
            } else {
                report = new AttendanceReport();
                report.setStudent(student);
                report.setCourse(course);
                report.setMonth(month);
                report.setYear(year);
            }
            
            report.setTotalClasses(totalClasses);
            report.setPresentCount(presentCount);
            report.setAbsentCount(absentCount);
            report.setLateCount(lateCount);
            report.setExcusedCount(excusedCount);
            report.setAttendancePercentage(percentage);
            report.setGeneratedAt(LocalDateTime.now());
            
            reportRepository.save(report);
            
            // Send low attendance alerts if needed
            if (percentage.compareTo(LOW_ATTENDANCE_THRESHOLD) < 0) {
                sendLowAttendanceAlert(student, course, percentage);
            }
        }
    }
    
    // Send low attendance alerts
    private void sendLowAttendanceAlert(User student, Course course, BigDecimal percentage) {
        String message = "Low attendance alert: Your attendance in " + course.getCourseName() 
            + " is " + percentage + "%, which is below the required 75%.";
        createAlert(student, message);
    }
    
    // Create attendance alert for student
    private void createAlert(User student, String message) {
        // In a real application, this would create an alert entry in the database
        // and potentially send an email or notification to the student
        System.out.println("Alert for " + student.getUsername() + ": " + message);
    }
    
    // Create attendance correction request from student
    @Transactional
    public AttendanceCorrectionRequest createCorrectionRequest(Course course, User student, 
                                                              String dateStr, String reason) {
        LocalDate date = LocalDate.parse(dateStr);
        
        // Find the attendance record for this date
        Optional<Attendance> attendanceOpt = 
            attendanceRepository.findByStudentAndCourseAndAttendanceDate(student, course, date);
            
        if (!attendanceOpt.isPresent()) {
            throw new IllegalArgumentException("No attendance record found for this date");
        }
        
        Attendance attendance = attendanceOpt.get();
        
        // Check if there's already a pending request
        List<AttendanceCorrectionRequest> existingRequests = 
            correctionRequestRepository.findByAttendance(attendance);
            
        for (AttendanceCorrectionRequest req : existingRequests) {
            if (req.getStatus() == AttendanceCorrectionRequest.RequestStatus.PENDING) {
                throw new IllegalStateException("A correction request is already pending for this date");
            }
        }
        
        // Create new correction request
        AttendanceCorrectionRequest request = new AttendanceCorrectionRequest();
        request.setAttendance(attendance);
        request.setRequestedBy(student);
        request.setReason(reason);
        request.setStatus(AttendanceCorrectionRequest.RequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        
        return correctionRequestRepository.save(request);
    }
    
    @Transactional
    public AttendanceCorrectionRequest createCorrectionRequest(Course course, User student, 
                                                               LocalDate date, String reason) {
        return createCorrectionRequest(course, student, date.toString(), reason);
    }
    
    // Submit student correction request
    @Transactional
    public AttendanceCorrectionRequest submitCorrectionRequest(Long attendanceId, 
                                                              User student, String reason) {
        // Implementation needed
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    // Methods for handling attendance correction requests
    @Transactional
    public AttendanceCorrectionRequest submitCorrectionRequest(Attendance attendance, 
                                                               User student, 
                                                               String reason) {
        // Check if a pending request already exists
        Optional<AttendanceCorrectionRequest> existingRequest = 
            correctionRequestRepository.findByAttendanceAndRequestedByAndStatus(
                attendance, student, AttendanceCorrectionRequest.RequestStatus.PENDING);
        
        if (existingRequest.isPresent()) {
            return existingRequest.get();
        }
        
        AttendanceCorrectionRequest request = new AttendanceCorrectionRequest();
        request.setAttendance(attendance);
        request.setRequestedBy(student);
        request.setReason(reason);
        request.setStatus(AttendanceCorrectionRequest.RequestStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        
        return correctionRequestRepository.save(request);
    }
    
    @Transactional
    public AttendanceCorrectionRequest reviewCorrectionRequest(Long requestId, 
                                                              AttendanceCorrectionRequest.RequestStatus status, 
                                                              User faculty, 
                                                              String comments) {
        AttendanceCorrectionRequest request = correctionRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        
        request.setStatus(status);
        request.setReviewedBy(faculty);
        request.setReviewComments(comments);
        request.setReviewedAt(LocalDateTime.now());
        
        AttendanceCorrectionRequest savedRequest = correctionRequestRepository.save(request);
        
        // If approved, update the attendance record
        if (status == AttendanceCorrectionRequest.RequestStatus.APPROVED) {
            Attendance attendance = request.getAttendance();
            attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            attendanceRepository.save(attendance);
        }
        
        return savedRequest;
    }
    
    // Get pending correction requests for a faculty
    public List<AttendanceCorrectionRequest> getPendingRequestsForFaculty(User faculty) {
        List<AttendanceCorrectionRequest> facultyRequests = 
            correctionRequestRepository.findByAttendance_MarkedBy(faculty);
        
        return facultyRequests.stream()
            .filter(req -> req.getStatus() == AttendanceCorrectionRequest.RequestStatus.PENDING)
            .collect(Collectors.toList());
    }
    
    // Get student's correction requests
    public List<AttendanceCorrectionRequest> getStudentCorrectionRequests(User student) {
        return correctionRequestRepository.findByRequestedBy(student);
    }
    
    // Methods for admin to see attendance data and reports
    public List<AttendanceReport> getCourseAttendanceReports(Course course, int month, int year) {
        return reportRepository.findByCourseAndMonthAndYear(course, month, year);
    }
    
    public List<AttendanceReport> getAllStudentsAttendanceReports(int month, int year) {
        List<AttendanceReport> reports = reportRepository.findAll();
        return reports.stream()
            .filter(report -> report.getMonth() == month && report.getYear() == year)
            .collect(Collectors.toList());
    }
} 