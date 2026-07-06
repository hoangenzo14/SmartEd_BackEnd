package com.smarted.ed.controller;

import com.smarted.ed.dto.RejectRequest;
import com.smarted.ed.dto.TutorSubjectRegisterRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TutorSubjectRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private User tutorUser;
    private TutorProfile tutorProfile;
    private Subject activeSubject;
    private Subject inactiveSubject;

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

        activeSubject = new Subject();
        activeSubject.setName("Math");
        activeSubject.setIsActive(true);
        entityManager.persist(activeSubject);

        inactiveSubject = new Subject();
        inactiveSubject.setName("Physics");
        inactiveSubject.setIsActive(false);
        entityManager.persist(inactiveSubject);

        tutorUser = new User();
        tutorUser.setEmail("tutor@example.com");
        tutorUser.setPassword("password");
        tutorUser.setFullName("Test Tutor");
        tutorUser.setRole(RoleType.TUTOR);
        tutorUser.setIsActive(true);
        tutorUser.setEmailVerified(true);
        entityManager.persist(tutorUser);

        tutorProfile = new TutorProfile();
        tutorProfile.setUser(tutorUser);
        tutorProfile.setApprovalStatus(ApprovalStatus.APPROVED);
        entityManager.persist(tutorProfile);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @WithMockUser(username = "tutor@example.com", roles = "TUTOR")
    public void testRegisterAndGetMySubjectsSuccess() throws Exception {
        // Register subject
        TutorSubjectRegisterRequest registerReq = new TutorSubjectRegisterRequest();
        registerReq.setSubjectId(activeSubject.getId());
        registerReq.setBio("Math bio description");
        registerReq.setHourlyRate(new BigDecimal("150.00"));
        registerReq.setCertificateUrl("http://example.com/cert.pdf");

        mockMvc.perform(post("/api/tutors/my-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Đăng ký môn học thành công")));

        entityManager.flush();
        entityManager.clear();

        // Get subjects list
        mockMvc.perform(get("/api/tutors/my-subjects")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].subjectName", is("Math")))
                .andExpect(jsonPath("$.data[0].status", is("PENDING")))
                .andExpect(jsonPath("$.data[0].bio", is("Math bio description")))
                .andExpect(jsonPath("$.data[0].hourlyRate", is(150.00)))
                .andExpect(jsonPath("$.data[0].certificateUrl", is("http://example.com/cert.pdf")));
    }

    @Test
    @WithMockUser(username = "tutor@example.com", roles = "TUTOR")
    public void testRegisterInactiveSubjectFails() throws Exception {
        TutorSubjectRegisterRequest registerReq = new TutorSubjectRegisterRequest();
        registerReq.setSubjectId(inactiveSubject.getId());
        registerReq.setBio("Physics bio");
        registerReq.setHourlyRate(new BigDecimal("120.00"));
        registerReq.setCertificateUrl("http://example.com/cert.pdf");

        mockMvc.perform(post("/api/tutors/my-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Môn học này hiện không hoạt động")));
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    public void testRegisterSubjectDeniedForNonTutor() throws Exception {
        TutorSubjectRegisterRequest registerReq = new TutorSubjectRegisterRequest();
        registerReq.setSubjectId(activeSubject.getId());
        registerReq.setBio("Math bio");
        registerReq.setHourlyRate(new BigDecimal("150.00"));
        registerReq.setCertificateUrl("http://example.com/cert.pdf");

        mockMvc.perform(post("/api/tutors/my-subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testApproveAndRejectTutorSubjectSuccess() throws Exception {
        // Setup initial pending TutorSubject relation
        TutorSubject ts = new TutorSubject();
        ts.setId(new TutorSubjectId(tutorProfile.getUserId(), activeSubject.getId()));
        ts.setTutor(tutorProfile);
        ts.setSubject(activeSubject);
        ts.setStatus(ApprovalStatus.PENDING);
        ts.setCertificateUrl("http://cert.pdf");
        
        // We need to fetch and save in transaction
        entityManager.persist(ts);
        entityManager.flush();
        entityManager.clear();

        // 1. Approve
        mockMvc.perform(put("/api/admin/tutors/" + tutorProfile.getUserId() + "/subjects/" + activeSubject.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Duyệt môn học của gia sư thành công")));

        entityManager.flush();
        entityManager.clear();
        TutorSubject approved = entityManager.find(TutorSubject.class, new TutorSubjectId(tutorProfile.getUserId(), activeSubject.getId()));
        assertEquals(ApprovalStatus.APPROVED, approved.getStatus());
        assertNull(approved.getRejectReason());

        // 2. Reject
        RejectRequest rejectReq = new RejectRequest();
        rejectReq.setReason("Incorrect certificate format");

        mockMvc.perform(put("/api/admin/tutors/" + tutorProfile.getUserId() + "/subjects/" + activeSubject.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Từ chối môn học của gia sư thành công")));

        entityManager.flush();
        entityManager.clear();
        TutorSubject rejected = entityManager.find(TutorSubject.class, new TutorSubjectId(tutorProfile.getUserId(), activeSubject.getId()));
        assertEquals(ApprovalStatus.REJECTED, rejected.getStatus());
        assertEquals("Incorrect certificate format", rejected.getRejectReason());
    }
}
