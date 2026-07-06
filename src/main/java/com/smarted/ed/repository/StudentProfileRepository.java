package com.smarted.ed.repository;

import com.smarted.ed.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Integer> {
    List<StudentProfile> findByParentId(Integer parentId);
}
