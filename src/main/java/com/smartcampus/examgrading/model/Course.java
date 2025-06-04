package com.smartcampus.examgrading.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id") 
    private Long id;

    @Column(name = "course_code", nullable = false, unique = true)
    private String courseCode;
    
    @Column(name = "course_name", nullable = false)
    private String courseName;
    
    private int capacity;
    
    private String content;
    
    @Column(name = "credit_hours", nullable = false)
    private int creditHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User faculty;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @Builder.Default
    private List<CourseMaterial> materials = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"course", "hibernateLazyInitializer", "handler"})
    @Builder.Default
    private List<Timetable> schedules = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Optional getter for compatibility with older references
    public Long getCourseId() {
        return this.id;
    }

    public void setCourseId(Long courseId) {
        this.id = courseId;
    }

    public boolean hasAvailableSeats() {
        return enrollments.stream().filter(Enrollment::isActive).count() < capacity;
    }

    public int getAvailableSeats() {
        return capacity - (int) enrollments.stream().filter(Enrollment::isActive).count();
    }

    public int getCurrentEnrollmentCount() {
        return (int) enrollments.stream().filter(Enrollment::isActive).count();
    }

    public String getCourseDescription() {
        return this.content;
    }

    public List<Timetable> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Timetable> schedules) {
        this.schedules = schedules;
    }

    public void addSchedule(Timetable schedule) {
        schedules.add(schedule);
        schedule.setCourse(this);
    }

    public void removeSchedule(Timetable schedule) {
        schedules.remove(schedule);
        schedule.setCourse(null);
    }
}
