package com.smarted.ed.repository;

import com.smarted.ed.entity.TutorSubject;
import com.smarted.ed.entity.TutorSubjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TutorSubjectRepository extends JpaRepository<TutorSubject, TutorSubjectId> {
    List<TutorSubject> findByTutorUserId(Integer tutorId);
}
