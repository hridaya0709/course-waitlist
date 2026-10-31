package dev.assessment.waitlist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "waitlist.course.capacity=1")
@AutoConfigureMockMvc
class CourseWaitlistControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void validatesRequestsAndPromotesThroughHttpApi() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .contentType("application/json")
                        .content("""
                                {"learnerId":"not-ready","prerequisiteSatisfied":false}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.outcome").value("PREREQUISITE_NOT_MET"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType("application/json")
                        .content("""
                                {"learnerId":" ","prerequisiteSatisfied":true}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/enrollments")
                        .contentType("application/json")
                        .content("""
                                {"learnerId":"first","prerequisiteSatisfied":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("ENROLLED"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType("application/json")
                        .content("""
                                {"learnerId":"first","prerequisiteSatisfied":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.outcome").value("ALREADY_ENROLLED"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType("application/json")
                        .content("""
                                {"learnerId":"second","prerequisiteSatisfied":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("WAITLISTED"));

        mockMvc.perform(post("/api/enrollments")
                        .contentType("application/json")
                        .content("""
                                {"learnerId":"second","prerequisiteSatisfied":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.outcome").value("ALREADY_WAITLISTED"));

        mockMvc.perform(delete("/api/enrollments/first"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promotedLearnerId").value("second"));

        mockMvc.perform(get("/api/course"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled[0]").value("second"))
                .andExpect(jsonPath("$.waitlist").isEmpty());

        mockMvc.perform(delete("/api/enrollments/first"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.outcome").value("NOT_ENROLLED"));
    }
}
