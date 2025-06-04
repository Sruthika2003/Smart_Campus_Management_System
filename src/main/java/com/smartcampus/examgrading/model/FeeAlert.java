package com.smartcampus.examgrading.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "fee_alerts")
public class FeeAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alertId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_fee_id", nullable = false)
    private StudentFee studentFee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounts_id", nullable = false)
    private User accounts;

    @Column(nullable = false)
    private LocalDateTime alertDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(columnDefinition = "LONGTEXT")
    private String message;

    public enum AlertType {
        EMAIL, SMS, SYSTEM
    }

    public enum Status {
        SENT, PENDING, FAILED
    }

    // Add constructor for easier creation
    public FeeAlert() {
        this.alertDate = LocalDateTime.now();
        this.alertType = AlertType.SYSTEM;
        this.status = Status.SENT;
    }
} 