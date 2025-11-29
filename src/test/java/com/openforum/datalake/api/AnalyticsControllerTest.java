package com.openforum.datalake.api;

import com.openforum.datalake.domain.DimMemberHealth;
import com.openforum.datalake.domain.DimThread;
import com.openforum.datalake.repository.DimMemberHealthRepository;
import com.openforum.datalake.repository.DimThreadRepository;
import com.openforum.datalake.repository.FactActivityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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
@AutoConfigureMockMvc(addFilters = false)
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
        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[] { "2023-10-01", 10L });
        when(factActivityRepository.countDailyActiveUsers(eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(stats);

        mockMvc.perform(get("/analytics/v1/activity")
                .param("tenantId", tenantId)
                .param("metric", "dau"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0][1]").value(10));
    }

    @Test
    void shouldGetStaleThreads() throws Exception {
        String tenantId = "tenant-1";
        DimThread thread = new DimThread();
        thread.setTitle("Stale Thread");
        when(dimThreadRepository.findStaleThreads(eq(tenantId), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(thread));

        mockMvc.perform(get("/analytics/v1/stale-threads")
                .param("tenantId", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Stale Thread"));
    }

    @Test
    void shouldGetChampions() throws Exception {
        String tenantId = "tenant-1";
        DimMemberHealth health = new DimMemberHealth();
        health.setEngagementLevel("CHAMPION");
        when(dimMemberHealthRepository.findByTenantIdAndEngagementLevel(tenantId, "CHAMPION"))
                .thenReturn(Collections.singletonList(health));

        mockMvc.perform(get("/analytics/v1/champions")
                .param("tenantId", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].engagementLevel").value("CHAMPION"));
    }
}
