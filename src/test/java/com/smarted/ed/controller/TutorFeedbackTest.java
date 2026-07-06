package com.smarted.ed.controller;

import com.smarted.ed.dto.FeedbackRequest;
import com.smarted.ed.entity.Appointment;
import com.smarted.ed.entity.StudentProfile;
import com.smarted.ed.entity.Subject;
import com.smarted.ed.entity.TutorProfile;
import com.smarted.ed.entity.User;
import com.smarted.ed.enums.AppointmentStatus;
import com.smarted.ed.enums.ApprovalStatus;
import com.smarted.ed.enums.RoleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TutorFeedbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private User parentUser;
    private User tutorUser;
    private TutorProfile tutorProfile;
    private StudentProfile studentProfile;
    private Subject subject;
    private Appointment completedAppointment;
    private Appointment pendingAppointment;

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

        // 1. Create Subject
        subject = new Subject();
        subject.setName("Math");
        subject.setIsActive(true);
        entityManager.persist(subject);

        // 2. Create Users (Parent & Tutor)
        parentUser = new User();
        parentUser.setEmail("parent@example.com");
        parentUser.setPassword("password");
        parentUser.setFullName("Test Parent");
        parentUser.setRole(RoleType.PARENT);
        parentUser.setIsActive(true);
        parentUser.setEmailVerified(true);
        entityManager.persist(parentUser);

        tutorUser = new User();
        tutorUser.setEmail("tutor@example.com");
        tutorUser.setPassword("password");
        tutorUser.setFullName("Test Tutor");
        tutorUser.setRole(RoleType.TUTOR);
        tutorUser.setIsActive(true);
        tutorUser.setEmailVerified(true);
        entityManager.persist(tutorUser);

        // 3. Create TutorProfile
        tutorProfile = new TutorProfile();
        tutorProfile.setUser(tutorUser);
        tutorProfile.setApprovalStatus(ApprovalStatus.APPROVED);
        tutorProfile.setAverageRating(BigDecimal.ZERO);
        entityManager.persist(tutorProfile);

        // 4. Create StudentProfile belonging to Parent
        studentProfile = new StudentProfile();
        studentProfile.setParentId(parentUser.getId());
        studentProfile.setFullName("Test Child");
        studentProfile.setGrade(10);
        entityManager.persist(studentProfile);

        // 5. Create Appointments
        completedAppointment = new Appointment();
        completedAppointment.setStudentId(studentProfile.getId());
        completedAppointment.setTutorId(tutorProfile.getUserId());
        completedAppointment.setSubjectId(subject.getId());
        completedAppointment.setStartTime(LocalDateTime.now().minusHours(3));
        completedAppointment.setEndTime(LocalDateTime.now().minusHours(1));
        completedAppointment.setStatus(AppointmentStatus.COMPLETED);
        entityManager.persist(completedAppointment);

        pendingAppointment = new Appointment();
        pendingAppointment.setStudentId(studentProfile.getId());
        pendingAppointment.setTutorId(tutorProfile.getUserId());
        pendingAppointment.setSubjectId(subject.getId());
        pendingAppointment.setStartTime(LocalDateTime.now().plusDays(1));
        pendingAppointment.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        pendingAppointment.setStatus(AppointmentStatus.PENDING);
        entityManager.persist(pendingAppointment);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    public void testSubmitFeedbackSuccess() throws Exception {
        FeedbackRequest request = new FeedbackRequest(completedAppointment.getId(), 5, "Tuyệt vời!");

        mockMvc.perform(post("/api/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.rating", is(5)))
                .andExpect(jsonPath("$.data.comment", is("Tuyệt vời!")))
                .andExpect(jsonPath("$.data.tutorName", is("Test Tutor")));

        // Check that average_rating of Tutor is updated to 5.00
        entityManager.flush();
        TutorProfile updatedProfile = entityManager.find(TutorProfile.class, tutorProfile.getUserId());
        assertEquals(0, updatedProfile.getAverageRating().compareTo(new BigDecimal("5.00")));
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    public void testSubmitFeedbackForNonCompletedAppointment() throws Exception {
        FeedbackRequest request = new FeedbackRequest(pendingAppointment.getId(), 4, "Khá ổn");

        mockMvc.perform(post("/api/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Chỉ có thể đánh giá lịch học đã hoàn thành")));
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    public void testSubmitDuplicateFeedback() throws Exception {
        FeedbackRequest request = new FeedbackRequest(completedAppointment.getId(), 4, "Lần đầu");

        mockMvc.perform(post("/api/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Submit again
        mockMvc.perform(post("/api/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("đã được đánh giá trước đó")));
    }

    @Test
    @WithMockUser(username = "tutor@example.com", roles = "TUTOR")
    public void testSubmitFeedbackAsTutorForbidden() throws Exception {
        FeedbackRequest request = new FeedbackRequest(completedAppointment.getId(), 5, "Tự khen");

        mockMvc.perform(post("/api/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    public void testGetFeedbackByAppointment() throws Exception {
        // First submit feedback
        FeedbackRequest request = new FeedbackRequest(completedAppointment.getId(), 4, "Tốt");
        mockMvc.perform(post("/api/feedbacks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then get
        mockMvc.perform(get("/api/feedbacks/appointment/" + completedAppointment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.rating", is(4)))
                .andExpect(jsonPath("$.data.comment", is("Tốt")));
    }

    @Test
    @WithMockUser(username = "parent@example.com", roles = "PARENT")
    public void testGetTutorDetailsWithBookedSlots() throws Exception {
        mockMvc.perform(get("/api/tutors/" + tutorProfile.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bookedSlots", notNullValue()))
                .andExpect(jsonPath("$.data.bookedSlots[0].status", is("BOOKED")));
    }

    @Test
    public void testGetFeedbacksByTutorPaginated() throws Exception {
        FeedbackRequest request = new FeedbackRequest(completedAppointment.getId(), 5, "Tuyệt vời!");
        
        mockMvc.perform(post("/api/feedbacks")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("parent@example.com").roles("PARENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/feedbacks/tutor/" + tutorProfile.getUserId())
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].rating", is(5)))
                .andExpect(jsonPath("$.data.content[0].comment", is("Tuyệt vời!")));
    }
}
