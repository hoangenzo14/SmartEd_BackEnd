// com.example.demo.repository.UserRepository.java
package com.smarted.ed.repository;
import com.smarted.ed.entity.User;
import com.smarted.ed.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(RoleType role);
    List<User> findByIsActiveTrue();
}