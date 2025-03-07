package com.rc.tracking.integration.controller;

import com.rc.tracking.TrackingApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TrackingApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TrackingEventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void whenValidTrackingEvent_thenReturnsAccepted() throws Exception {
        String trackingEventJson = """
                {
                    "packageId": "packageEntity-1",
                    "location": "Warehouse",
                    "description": "Package reached warehouse",
                    "date": "2025-10-10T12:00:00"
                }
                """;

        mockMvc.perform(post("/api/tracking-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(trackingEventJson))
                .andExpect(status().isAccepted());
    }

    @Test
    public void whenInvalidTrackingEvent_thenReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                    "packageId": "",
                    "location": "Warehouse",
                    "description": "Package reached warehouse",
                    "date": null
                }
                """;

        mockMvc.perform(post("/api/tracking-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}