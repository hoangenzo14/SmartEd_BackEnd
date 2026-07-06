package com.smarted.ed.controller;

import com.smarted.ed.entity.Subject;
import com.smarted.ed.entity.TutorProfile;
import com.smarted.ed.entity.TutorSubject;
import com.smarted.ed.entity.TutorSubjectId;
import com.smarted.ed.entity.User;
import com.smarted.ed.enums.ApprovalStatus;
import com.smarted.ed.enums.RoleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TutorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private User tutorUser1;
    private User tutorUser2;
    private User pendingUser;

    private TutorProfile approvedProfile1;
    private TutorProfile approvedProfile2;
    private TutorProfile pendingProfile;

    private Subject mathSubject;
    private Subject englishSubject;

    @BeforeEach
    public void setup() {
        // Clear database to isolate from data.sql seed data
        entityManager.createQuery("DELETE FROM Transaction").executeUpdate();
        entityManager.createQuery("DELETE FROM Feedback").executeUpdate();
        entityManager.createQuery("DELETE FROM TutorSubject").executeUpdate();
        entityManager.createQuery("DELETE FROM Appointment").executeUpdate();
        entityManager.createQuery("DELETE FROM TutorProfile").executeUpdate();
        entityManager.createQuery("DELETE FROM VerificationToken").executeUpdate();
        entityManager.createQuery("DELETE FROM StudentProfile").executeUpdate();
        entityManager.createQuery("DELETE FROM User").executeUpdate();
        entityManager.createQuery("DELETE FROM Subject").executeUpdate();
        entityManager.flush();

        // Create subjects
        mathSubject = new Subject();
        mathSubject.setName("Math");
        mathSubject.setIsActive(true);
        entityManager.persist(mathSubject);

        englishSubject = new Subject();
        englishSubject.setName("English");
        englishSubject.setIsActive(true);
        entityManager.persist(englishSubject);

        // Tutor 1: Approved, Math, Rating 4.8, Rate 200
        tutorUser1 = new User();
        tutorUser1.setEmail("math_tutor@example.com");
        tutorUser1.setPassword("password");
        tutorUser1.setFullName("Math Tutor Nguyen");
        tutorUser1.setRole(RoleType.TUTOR);
        tutorUser1.setIsActive(true);
        tutorUser1.setEmailVerified(true);
        entityManager.persist(tutorUser1);

        approvedProfile1 = new TutorProfile();
        approvedProfile1.setUser(tutorUser1);
        approvedProfile1.setLocation("Hanoi");
        approvedProfile1.setAverageRating(new BigDecimal("4.8"));
        approvedProfile1.setApprovalStatus(ApprovalStatus.APPROVED);
        entityManager.persist(approvedProfile1);

        TutorSubject ts1 = new TutorSubject();
        ts1.setId(new TutorSubjectId(approvedProfile1.getUserId(), mathSubject.getId()));
        ts1.setTutor(approvedProfile1);
        ts1.setSubject(mathSubject);
        ts1.setStatus(ApprovalStatus.APPROVED);
        ts1.setBio("Experienced math tutor");
        ts1.setHourlyRate(new BigDecimal("200.00"));
        entityManager.persist(ts1);

        // Tutor 2: Approved, English, Rating 4.5, Rate 150
        tutorUser2 = new User();
        tutorUser2.setEmail("english_tutor@example.com");
        tutorUser2.setPassword("password");
        tutorUser2.setFullName("English Tutor Tran");
        tutorUser2.setRole(RoleType.TUTOR);
        tutorUser2.setIsActive(true);
        tutorUser2.setEmailVerified(true);
        entityManager.persist(tutorUser2);

        approvedProfile2 = new TutorProfile();
        approvedProfile2.setUser(tutorUser2);
        approvedProfile2.setLocation("Hanoi");
        approvedProfile2.setAverageRating(new BigDecimal("4.5"));
        approvedProfile2.setApprovalStatus(ApprovalStatus.APPROVED);
        entityManager.persist(approvedProfile2);

        TutorSubject ts2 = new TutorSubject();
        ts2.setId(new TutorSubjectId(approvedProfile2.getUserId(), englishSubject.getId()));
        ts2.setTutor(approvedProfile2);
        ts2.setSubject(englishSubject);
        ts2.setStatus(ApprovalStatus.APPROVED);
        ts2.setBio("English communication and IELTS");
        ts2.setHourlyRate(new BigDecimal("150.00"));
        entityManager.persist(ts2);

        // Tutor 3: Pending, Math, Rating 4.0, Rate 100
        pendingUser = new User();
        pendingUser.setEmail("pending_tutor@example.com");
        pendingUser.setPassword("password");
        pendingUser.setFullName("Pending Tutor Le");
        pendingUser.setRole(RoleType.TUTOR);
        pendingUser.setIsActive(true);
        pendingUser.setEmailVerified(true);
        entityManager.persist(pendingUser);

        pendingProfile = new TutorProfile();
        pendingProfile.setUser(pendingUser);
        pendingProfile.setLocation("Hanoi");
        pendingProfile.setAverageRating(new BigDecimal("4.0"));
        pendingProfile.setApprovalStatus(ApprovalStatus.PENDING);
        entityManager.persist(pendingProfile);

        TutorSubject ts3 = new TutorSubject();
        ts3.setId(new TutorSubjectId(pendingProfile.getUserId(), mathSubject.getId()));
        ts3.setTutor(pendingProfile);
        ts3.setSubject(mathSubject);
        ts3.setStatus(ApprovalStatus.APPROVED);
        ts3.setBio("Pending tutor bio");
        ts3.setHourlyRate(new BigDecimal("100.00"));
        entityManager.persist(ts3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void testGetApprovedTutorsSuccess() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Lấy danh sách gia sư thành công")))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].fullName", is("Math Tutor Nguyen")))
                .andExpect(jsonPath("$.data.content[1].fullName", is("English Tutor Tran")));
    }

    @Test
    public void testGetApprovedTutorsSearchByName() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .param("search", "Nguyen")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName", is("Math Tutor Nguyen")));
    }

    @Test
    public void testGetApprovedTutorsSearchBySubject() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .param("search", "English")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName", is("English Tutor Tran")))
                .andExpect(jsonPath("$.data.content[0].subjects[0]", is("English")));
    }

    @Test
    public void testGetApprovedTutorsSortByPriceAsc() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .param("sortBy", "price")
                        .param("sortDir", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].fullName", is("English Tutor Tran"))) // 150
                .andExpect(jsonPath("$.data.content[1].fullName", is("Math Tutor Nguyen"))); // 200
    }

    @Test
    public void testGetApprovedTutorsSortByRatingDesc() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .param("sortBy", "rating")
                        .param("sortDir", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].fullName", is("Math Tutor Nguyen"))) // 4.8
                .andExpect(jsonPath("$.data.content[1].fullName", is("English Tutor Tran"))); // 4.5
    }
}
