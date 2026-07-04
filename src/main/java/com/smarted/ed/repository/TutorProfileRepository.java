// com.example.demo.repository.TutorProfileRepository.java
package com.smarted.ed.repository;

import com.smarted.ed.entity.TutorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfile, Integer> {
    List<TutorProfile> findByLocationContaining(String location);
    List<TutorProfile> findByHourlyRateLessThanEqual(BigDecimal maxRate);

    @Query("SELECT t FROM TutorProfile t WHERE t.averageRating >= :minRating")
    List<TutorProfile> findByAverageRatingGreaterThanEqual(BigDecimal minRating);
}