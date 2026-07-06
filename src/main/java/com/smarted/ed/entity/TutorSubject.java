// com.example.demo.entity.TutorSubject.java
package com.smarted.ed.entity;

import com.smarted.ed.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tutor_subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorSubject {

    @EmbeddedId
    private TutorSubjectId id;

    @MapsId("tutorId")
    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private TutorProfile tutor;

    @MapsId("subjectId")
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "certificate_url")
    private String certificateUrl;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
