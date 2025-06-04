package com.smartcampus.examgrading.controller;

import com.smartcampus.examgrading.model.Timetable;
import com.smartcampus.examgrading.service.TimetableService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getSchedulesForCourse(@PathVariable Long courseId) {
        try {
            List<Timetable> schedules = timetableService.getSchedulesForCourse(courseId);
            return ResponseEntity.ok(schedules);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveSchedule(@RequestBody Timetable schedule) {
        try {
            Timetable savedSchedule = timetableService.saveSchedule(schedule);
            return ResponseEntity.ok(savedSchedule);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        try {
            timetableService.deleteSchedule(id);
            return ResponseEntity.ok(Map.of("message", "Schedule deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getScheduleById(@PathVariable Long id) {
        try {
            Timetable schedule = timetableService.getScheduleById(id);
            return ResponseEntity.ok(schedule);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Timetable>> getAllSchedules() {
        return ResponseEntity.ok(timetableService.getAllSchedules());
    }
} 