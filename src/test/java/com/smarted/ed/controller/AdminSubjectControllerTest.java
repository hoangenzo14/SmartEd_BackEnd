package com.smarted.ed.controller;

import com.smarted.ed.dto.SubjectRequest;
import com.smarted.ed.entity.Subject;
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

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AdminSubjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

    private Subject subject1;
    private Subject subject2;

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

        subject1 = new Subject();
        subject1.setName("Math");
        subject1.setDescription("Mathematics");
        subject1.setIsActive(true);
        entityManager.persist(subject1);

        subject2 = new Subject();
        subject2.setName("English");
        subject2.setDescription("English language");
        subject2.setIsActive(false);
        entityManager.persist(subject2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testGetAllSubjectsSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/subjects")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testSearchSubjectsSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/subjects")
                        .param("search", "Math")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("Math")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testGetSubjectByIdSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/subjects/" + subject1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Math")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testCreateSubjectSuccess() throws Exception {
        SubjectRequest request = new SubjectRequest();
        request.setName("Science");
        request.setDescription("Natural Sciences");
        request.setIsActive(true);

        mockMvc.perform(post("/api/admin/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Science")));

        entityManager.flush();
        entityManager.clear();
        List<Subject> all = entityManager.createQuery("SELECT s FROM Subject s WHERE s.name = 'Science'", Subject.class).getResultList();
        assertEquals(1, all.size());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testCreateSubjectDuplicateName() throws Exception {
        SubjectRequest request = new SubjectRequest();
        request.setName("Math");

        mockMvc.perform(post("/api/admin/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Tên môn học 'Math' đã tồn tại")));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testUpdateSubjectSuccess() throws Exception {
        SubjectRequest request = new SubjectRequest();
        request.setName("Mathematics");
        request.setDescription("Advanced Math");
        request.setIsActive(true);

        mockMvc.perform(put("/api/admin/subjects/" + subject1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Mathematics")));

        entityManager.flush();
        entityManager.clear();
        Subject updated = entityManager.find(Subject.class, subject1.getId());
        assertEquals("Mathematics", updated.getName());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    public void testDeleteSubjectSuccess() throws Exception {
        mockMvc.perform(delete("/api/admin/subjects/" + subject1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        entityManager.flush();
        entityManager.clear();
        Subject deleted = entityManager.find(Subject.class, subject1.getId());
        assertNull(deleted);
    }

    @Test
    @WithMockUser(username = "tutor@example.com", roles = "TUTOR")
    public void testAccessDeniedForNonAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/subjects")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
