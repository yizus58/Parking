package com.nelumbo.park.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SonarQubeController.class)
class SonarQubeControllerTest extends WebLayerTestSupport {

    @Test
    @WithMockUser(authorities = "ADMIN")
    void admin_readsTheLatestPayloads_boundedToMax() throws Exception {
        for (int i = 0; i < SonarQubeController.MAX_PAYLOADS + 5; i++) {
            mockMvc.perform(post("/api/sonarqube-webhook").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"n\":" + i + "}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/sonarqube-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(SonarQubeController.MAX_PAYLOADS))
                .andExpect(jsonPath("$[0].n").value(5))
                .andExpect(jsonPath("$[49].n").value(SonarQubeController.MAX_PAYLOADS + 4));
    }

    @Test
    @WithMockUser(authorities = "SOCIO")
    void partner_cannotReadThePayloads() throws Exception {
        mockMvc.perform(get("/api/sonarqube-logs"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymous_cannotPostOrRead() throws Exception {
        mockMvc.perform(post("/api/sonarqube-webhook").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/sonarqube-logs"))
                .andExpect(status().isUnauthorized());
    }
}
