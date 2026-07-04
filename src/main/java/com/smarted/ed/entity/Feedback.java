// com.example.demo.entity.Feedback.java
package com.smarted.ed.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "appointment_id", unique = true, nullable = false)
    private Integer appointmentId;

    @OneToOne
    @JoinColumn(name = "appointment_id", insertable = false, updatable = false)
    private Appointment appointment;

    @Column(name = "parent_id", nullable = false)
    private Integer parentId;

    @ManyToOne
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private User parent;

    @Column(name = "tutor_id", nullable = false)
    private Integer tutorId;

    @ManyToOne
    @JoinColumn(name = "tutor_id", insertable = false, updatable = false)
    private TutorProfile tutor;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}