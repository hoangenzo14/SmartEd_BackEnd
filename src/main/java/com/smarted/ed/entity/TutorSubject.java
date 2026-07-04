// com.example.demo.entity.TutorSubject.java
package com.smarted.ed.entity;

import com.smarted.ed.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "tutor_subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TutorSubject {

    @EmbeddedId
    private TutorSubjectId id;  // 👈 Nhúng ID vào bên trong entity

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

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

// TutorSubjectId.java - Vẫn cần nhưng nằm trong cùng file
@Embeddable
class TutorSubjectId implements Serializable {
    @Column(name = "tutor_id")
    private Integer tutorId;

    @Column(name = "subject_id")
    private Integer subjectId;

    public TutorSubjectId() {}

    public TutorSubjectId(Integer tutorId, Integer subjectId) {
        this.tutorId = tutorId;
        this.subjectId = subjectId;
    }

    // Getter, Setter, equals, hashCode
}