package com.rc.tracking.load;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("load")
public class HighLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHighLoadConcurrentTrackingEventRequests() throws Exception {
        int numberOfThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        String jsonRequest = """
                {
                    "packageId": "packageEntity-1",
                    "location": "Warehouse",
                    "description": "Package reached warehouse",
                    "date": "2025-10-10T12:00:00"
                }
                """;

        for (int i = 0; i < numberOfThreads; i++) {
            Future<Integer> future = executor.submit(() -> {
                try {
                    latch.await();
                    return mockMvc.perform(post("/api/tracking-events")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonRequest))
                            .andReturn().getResponse().getStatus();
                } catch (Exception e) {
                    e.printStackTrace();
                    return 500;
                }
            });
            futures.add(future);
        }

        latch.countDown();

        int acceptedCount = 0;
        int errorCount = 0;
        for (Future<Integer> future : futures) {
            int status = future.get();
            if (status == 202) {
                acceptedCount++;
            } else {
                errorCount++;
            }
        }
        executor.shutdown();

        System.out.println("Accepted: " + acceptedCount);
        System.out.println("Errors: " + errorCount);

        int total = acceptedCount + errorCount;
        double successRate = acceptedCount / (double) total;
        assert(successRate >= 0.9);
    }
}
