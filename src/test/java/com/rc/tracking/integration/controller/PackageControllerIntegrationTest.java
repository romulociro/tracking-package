package com.rc.tracking.integration.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PackageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCreatePackageEndpoint() throws Exception {
        String jsonRequest = """
                {
                    "description": "Test Package",
                    "sender": "Sender A",
                    "recipient": "Recipient B",
                    "estimatedDeliveryDate": "2025-10-10"
                }
                """;

        mockMvc.perform(post("/api/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    public void testTrackingEventEndpointAsync() throws Exception {
        String jsonRequest = """
                {
                    "packageId": "packageEntity-1",
                    "location": "Warehouse",
                    "description": "Package reached warehouse",
                    "date": "2025-10-10T12:00:00"
                }
                """;

        mockMvc.perform(post("/api/tracking-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isAccepted());
    }
}