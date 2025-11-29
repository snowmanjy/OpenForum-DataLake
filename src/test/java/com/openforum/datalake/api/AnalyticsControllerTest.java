package com.openforum.datalake.api;

import com.openforum.datalake.domain.DimMemberHealth;
import com.openforum.datalake.domain.EngagementLevel;
import com.openforum.datalake.domain.DimThread;
import com.openforum.datalake.dto.DailyActiveUsersPoint;
import com.openforum.datalake.repository.DimMemberHealthRepository;
import com.openforum.datalake.repository.DimThreadRepository;
import com.openforum.datalake.repository.FactActivityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private FactActivityRepository factActivityRepository;

        @MockBean
        private DimThreadRepository dimThreadRepository;

        @MockBean
        private DimMemberHealthRepository dimMemberHealthRepository;

        @Test
        void shouldGetActivity() throws Exception {
                String tenantId = "tenant-1";
                List<DailyActiveUsersPoint> stats = new ArrayList<>();
                stats.add(new DailyActiveUsersPoint(LocalDate.parse("2023-10-01"), 10L));
                when(factActivityRepository.countDailyActiveUsers(eq(tenantId), any(Instant.class)))
                                .thenReturn(stats);

                mockMvc.perform(get("/analytics/v1/activity")
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.claim("tenant_id", tenantId)))
                                .param("metric", "dau"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].date").value("2023-10-01"))
                                .andExpect(jsonPath("$[0].count").value(10));
        }

        @Test
        void shouldGetResponsiveness() throws Exception {
                String tenantId = "tenant-1";
                when(dimThreadRepository.countTotalThreads(tenantId)).thenReturn(100L);
                when(dimThreadRepository.countAnsweredThreads(tenantId)).thenReturn(80L);

                mockMvc.perform(get("/analytics/v1/responsiveness")
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.claim("tenant_id", tenantId))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.answerRate").value(0.8))
                                .andExpect(jsonPath("$.medianResponseTimeMinutes").value(0));
        }

        @Test
        void shouldGetDeflectionSavings() throws Exception {
                String tenantId = "tenant-1";
                when(dimThreadRepository.countAnsweredThreads(tenantId)).thenReturn(100L);

                mockMvc.perform(get("/analytics/v1/deflection-savings")
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.claim("tenant_id", tenantId))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.deflectionCount").value(100))
                                .andExpect(jsonPath("$.estimatedSavings").value(5000.0));
        }

        @Test
        void shouldGetStaleThreads() throws Exception {
                String tenantId = "tenant-1";
                DimThread thread = new DimThread();
                thread.setTitle("Stale Thread");
                when(dimThreadRepository.findStaleThreads(eq(tenantId), any(Instant.class)))
                                .thenReturn(Collections.singletonList(thread));

                mockMvc.perform(get("/analytics/v1/stale-threads")
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.claim("tenant_id", tenantId))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Stale Thread"));
        }

        @Test
        void shouldGetChampions() throws Exception {
                String tenantId = "tenant-1";
                DimMemberHealth health = new DimMemberHealth();
                health.setEngagementLevel(EngagementLevel.CHAMPION);
                when(dimMemberHealthRepository.findByTenantIdAndEngagementLevel(tenantId, "CHAMPION"))
                                .thenReturn(Collections.singletonList(health));

                mockMvc.perform(get("/analytics/v1/champions")
                                .with(SecurityMockMvcRequestPostProcessors.jwt()
                                                .jwt(jwt -> jwt.claim("tenant_id", tenantId))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].engagementLevel").value("CHAMPION"));
        }
}
