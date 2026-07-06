package com.smarted.ed.repository;

import com.smarted.ed.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    Optional<Feedback> findByAppointmentId(Integer appointmentId);

    Page<Feedback> findByTutorId(Integer tutorId, Pageable pageable);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.tutorId = :tutorId")
    Double getAverageRatingForTutor(@Param("tutorId") Integer tutorId);
}
