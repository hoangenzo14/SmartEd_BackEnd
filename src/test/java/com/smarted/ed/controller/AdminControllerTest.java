package com.smarted.ed.controller;

import com.smarted.ed.dto.RejectRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private User user1;
    private User user2;
    private User user3;

    private TutorProfile profile1;
    private TutorProfile profile2;
    private TutorProfile profile3;

    private Subject mathSubject;
    private Subject englishSubject;

    @BeforeEach
    public void setup() {
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

        // Tutor 1: Approved, Math, Rating 4.8, Rate 200, name "Tutor One"
        user1 = new User();
        user1.setEmail("tutor1@example.com");
        user1.setPassword("password");
        user1.setFullName("Tutor One Nguyen");
        user1.setRole(RoleType.TUTOR);
        user1.setIsActive(true);
        user1.setEmailVerified(true);
        entityManager.persist(user1);

        profile1 = new TutorProfile();
        profile1.setUser(user1);
        profile1.setLocation("Hanoi");
        profile1.setAverageRating(new BigDecimal("4.8"));
        profile1.setApprovalStatus(ApprovalStatus.APPROVED);
        entityManager.persist(profile1);

        TutorSubject ts1 = new TutorSubject();
        ts1.setId(new TutorSubjectId(profile1.getUserId(), mathSubject.getId()));
        ts1.setTutor(profile1);
        ts1.setSubject(mathSubject);
        ts1.setStatus(ApprovalStatus.APPROVED);
        ts1.setBio("Math expert");
        ts1.setHourlyRate(new BigDecimal("200.00"));
        entityManager.persist(ts1);

        // Tutor 2: Pending, English, Rating 4.0, Rate 100, name "Tutor Two"
        user2 = new User();
        user2.setEmail("tutor2@example.com");
        user2.setPassword("password");
        user2.setFullName("Tutor Two Tran");
        user2.setRole(RoleType.TUTOR);
        user2.setIsActive(true);
        user2.setEmailVerified(true);
        entityManager.persist(user2);

        profile2 = new TutorProfile();
        profile2.setUser(user2);
        profile2.setLocation("Da Nang");
        profile2.setAverageRating(new BigDecimal("4.0"));
        profile2.setApprovalStatus(ApprovalStatus.PENDING);
        entityManager.persist(profile2);

        TutorSubject ts2 = new TutorSubject();
        ts2.setId(new TutorSubjectId(profile2.getUserId(), englishSubject.getId()));
        ts2.setTutor(profile2);
        ts2.setSubject(englishSubject);
        ts2.setStatus(ApprovalStatus.PENDING);
        ts2.setBio("English conversationalist");
        ts2.setHourlyRate(new BigDecimal("100.00"));
        entityManager.persist(ts2);

        // Tutor 3: Rejected, Math, Rating 3.5, Rate 120, name "Tutor Three"
        user3 = new User();
        user3.setEmail("tutor3@example.com");
        user3.setPassword("password");
        user3.setFullName("Tutor Three Le");
        user3.setRole(RoleType.TUTOR);
        user3.setIsActive(true);
        user3.setEmailVerified(true);
        entityManager.persist(user3);

        profile3 = new TutorProfile();
        profile3.setUser(user3);
        profile3.setLocation("HCMC");
        profile3.setAverageRating(new BigDecimal("3.5"));
        profile3.setApprovalStatus(ApprovalStatus.REJECTED);
        profile3.setRejectReason("Old profile");
        entityManager.persist(profile3);

        TutorSubject ts3 = new TutorSubject();
        ts3.setId(new TutorSubjectId(profile3.getUserId(), mathSubject.getId()));
        ts3.setTutor(profile3);
        ts3.setSubject(mathSubject);
        ts3.setStatus(ApprovalStatus.REJECTED);
        ts3.setBio("Another math tutor");
        ts3.setHourlyRate(new BigDecimal("120.00"));
        entityManager.persist(ts3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testGetAllTutorsForAdminSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/tutors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(3)));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testGetAllTutorsFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/admin/tutors")
                        .param("status", "PENDING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName", is("Tutor Two Tran")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testGetAllTutorsSearchByEmail() throws Exception {
        mockMvc.perform(get("/api/admin/tutors")
                        .param("search", "tutor3@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].fullName", is("Tutor Three Le")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testGetTutorByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/tutors/" + profile2.getUserId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Tutor Two Tran")))
                .andExpect(jsonPath("$.data.subjects", hasSize(1)))
                .andExpect(jsonPath("$.data.subjects[0].subjectName", is("English")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testApproveTutorSubjectSuccessUpdatesProfileStatus() throws Exception {
        mockMvc.perform(put("/api/admin/tutors/" + profile2.getUserId() + "/subjects/" + englishSubject.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Duyệt môn học của gia sư thành công")));

        // Verify state is committed to DB
        entityManager.flush();
        entityManager.clear();
        TutorProfile updated = entityManager.find(TutorProfile.class, profile2.getUserId());
        assertNotNull(updated);
        assertEquals(ApprovalStatus.APPROVED, updated.getApprovalStatus()); // Aggregated status must be APPROVED (at least one approved subject)
        assertNull(updated.getRejectReason());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testRejectTutorSubjectSuccessUpdatesProfileStatus() throws Exception {
        RejectRequest request = new RejectRequest();
        request.setReason("Incomplete certificate details");

        mockMvc.perform(put("/api/admin/tutors/" + profile2.getUserId() + "/subjects/" + englishSubject.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Từ chối môn học của gia sư thành công")));

        // Verify state is committed to DB
        entityManager.flush();
        entityManager.clear();
        TutorProfile updated = entityManager.find(TutorProfile.class, profile2.getUserId());
        assertNotNull(updated);
        assertEquals(ApprovalStatus.REJECTED, updated.getApprovalStatus()); // Aggregated status must be REJECTED (all subjects rejected)
        assertNotNull(updated.getRejectReason());
    }

    @Test
    @WithMockUser(username = "tutor@example.com", roles = "TUTOR")
    public void testAccessDeniedForTutorRole() throws Exception {
        mockMvc.perform(get("/api/admin/tutors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    public void testAccessDeniedForParentRole() throws Exception {
        mockMvc.perform(get("/api/admin/tutors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testAccessDeniedForGuest() throws Exception {
        mockMvc.perform(get("/api/admin/tutors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testGetAdminRevenueApis() throws Exception {
        mockMvc.perform(get("/api/admin/revenue/monthly")
                        .param("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(12)));

        mockMvc.perform(get("/api/admin/revenue/subjects")
                        .param("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(notNullValue())));
    }
}
