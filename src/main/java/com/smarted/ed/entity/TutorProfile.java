// com.example.demo.entity.TutorProfile.java
package com.smarted.ed.entity;

import com.smarted.ed.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "tutor_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorProfile {
    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String location;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "average_rating")
    private BigDecimal averageRating = BigDecimal.ZERO;

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL)
    private List<TutorSubject> tutorSubjects;

    @OneToMany(mappedBy = "tutor")
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "tutor")
    private List<Feedback> feedbacks;
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "reject_reason")
    private String rejectReason;
}