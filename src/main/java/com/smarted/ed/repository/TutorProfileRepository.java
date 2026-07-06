package com.smarted.ed.repository;

import com.smarted.ed.entity.TutorProfile;
import com.smarted.ed.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfile, Integer> {
    List<TutorProfile> findByLocationContaining(String location);

    @Query("SELECT t FROM TutorProfile t WHERE t.averageRating >= :minRating")
    List<TutorProfile> findByAverageRatingGreaterThanEqual(BigDecimal minRating);

    @Query("SELECT t FROM TutorProfile t " +
           "LEFT JOIN FETCH t.user u " +
           "WHERE t.approvalStatus = :status " +
           "AND (:search IS NULL OR :search = '' " +
           "    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "    OR EXISTS (SELECT ts FROM TutorSubject ts JOIN ts.subject s WHERE ts.tutor.userId = t.userId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY " +
           "  CASE WHEN :sortBy = 'price' AND :sortDir = 'asc' THEN (SELECT COALESCE(MIN(ts2.hourlyRate), 0) FROM TutorSubject ts2 WHERE ts2.tutor.userId = t.userId) END ASC, " +
           "  CASE WHEN :sortBy = 'price' AND :sortDir = 'desc' THEN (SELECT COALESCE(MIN(ts2.hourlyRate), 0) FROM TutorSubject ts2 WHERE ts2.tutor.userId = t.userId) END DESC, " +
           "  CASE WHEN :sortBy = 'rating' AND :sortDir = 'asc' THEN t.averageRating END ASC, " +
           "  CASE WHEN :sortBy = 'rating' AND :sortDir = 'desc' THEN t.averageRating END DESC, " +
           "  t.userId DESC")
    Page<TutorProfile> findByApprovalStatusAndSearch(
            @Param("status") ApprovalStatus status,
            @Param("search") String search,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            Pageable pageable
    );

    @Query("SELECT t FROM TutorProfile t " +
           "LEFT JOIN FETCH t.user u " +
           "WHERE (:status IS NULL OR t.approvalStatus = :status) " +
           "AND (:search IS NULL OR :search = '' " +
           "    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "    OR EXISTS (SELECT ts FROM TutorSubject ts JOIN ts.subject s WHERE ts.tutor.userId = t.userId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY " +
           "  CASE WHEN :sortBy = 'price' AND :sortDir = 'asc' THEN (SELECT COALESCE(MIN(ts2.hourlyRate), 0) FROM TutorSubject ts2 WHERE ts2.tutor.userId = t.userId) END ASC, " +
           "  CASE WHEN :sortBy = 'price' AND :sortDir = 'desc' THEN (SELECT COALESCE(MIN(ts2.hourlyRate), 0) FROM TutorSubject ts2 WHERE ts2.tutor.userId = t.userId) END DESC, " +
           "  CASE WHEN :sortBy = 'rating' AND :sortDir = 'asc' THEN t.averageRating END ASC, " +
           "  CASE WHEN :sortBy = 'rating' AND :sortDir = 'desc' THEN t.averageRating END DESC, " +
           "  t.userId DESC")
    Page<TutorProfile> findAllTutorsForAdmin(
            @Param("status") ApprovalStatus status,
            @Param("search") String search,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            Pageable pageable
    );

    @Query(value = "SELECT DISTINCT t FROM TutorProfile t " +
           "JOIN FETCH t.user u " +
           "JOIN FETCH t.tutorSubjects ts " +
           "JOIN FETCH ts.subject s " +
           "WHERE u.isActive = true " +
           "AND t.approvalStatus = 'APPROVED' " +
           "AND ts.status = 'APPROVED' " +
           "AND (:search IS NULL OR :search = '' " +
           "    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "    OR LOWER(ts.subject.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:subjectId IS NULL OR ts.subject.id = :subjectId) " +
           "AND (:minPrice IS NULL OR ts.hourlyRate >= :minPrice) " +
           "AND (:maxPrice IS NULL OR ts.hourlyRate <= :maxPrice) " +
           "AND (:minRating IS NULL OR t.averageRating >= :minRating) " +
           "ORDER BY " +
           "  CASE WHEN :sortBy = 'price' AND :sortDir = 'asc' THEN (SELECT COALESCE(MIN(ts2.hourlyRate), 0) FROM TutorSubject ts2 WHERE ts2.tutor.userId = t.userId AND ts2.status = 'APPROVED') END ASC, " +
           "  CASE WHEN :sortBy = 'price' AND :sortDir = 'desc' THEN (SELECT COALESCE(MIN(ts2.hourlyRate), 0) FROM TutorSubject ts2 WHERE ts2.tutor.userId = t.userId AND ts2.status = 'APPROVED') END DESC, " +
           "  CASE WHEN :sortBy = 'rating' AND :sortDir = 'asc' THEN t.averageRating END ASC, " +
           "  CASE WHEN :sortBy = 'rating' AND :sortDir = 'desc' THEN t.averageRating END DESC, " +
           "  t.userId DESC",
           countQuery = "SELECT COUNT(DISTINCT t) FROM TutorProfile t " +
                        "JOIN t.user u " +
                        "JOIN t.tutorSubjects ts " +
                        "WHERE u.isActive = true " +
                        "AND t.approvalStatus = 'APPROVED' " +
                        "AND ts.status = 'APPROVED' " +
                        "AND (:search IS NULL OR :search = '' " +
                        "    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "    OR LOWER(ts.subject.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                        "AND (:subjectId IS NULL OR ts.subject.id = :subjectId) " +
                        "AND (:minPrice IS NULL OR ts.hourlyRate >= :minPrice) " +
                        "AND (:maxPrice IS NULL OR ts.hourlyRate <= :maxPrice) " +
                        "AND (:minRating IS NULL OR t.averageRating >= :minRating)")
    Page<TutorProfile> searchTutors(
            @Param("search") String search,
            @Param("subjectId") Integer subjectId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") BigDecimal minRating,
            @Param("sortBy") String sortBy,
            @Param("sortDir") String sortDir,
            Pageable pageable
    );

    @Query("SELECT DISTINCT t FROM TutorProfile t " +
           "LEFT JOIN FETCH t.user u " +
           "LEFT JOIN FETCH t.tutorSubjects ts " +
           "LEFT JOIN FETCH ts.subject s " +
           "WHERE t.userId = :id")
    java.util.Optional<TutorProfile> findByIdWithDetails(@Param("id") Integer id);
}