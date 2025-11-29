package com.openforum.datalake.job;

import com.openforum.datalake.domain.DimMemberHealth;
import com.openforum.datalake.repository.DimMemberHealthRepository;
import com.openforum.datalake.repository.FactActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberHealthCalculationJobTest {

    @Mock
    private FactActivityRepository factActivityRepository;

    @Mock
    private DimMemberHealthRepository dimMemberHealthRepository;

    private MemberHealthCalculationJob job;

    @BeforeEach
    void setUp() {
        job = new MemberHealthCalculationJob(factActivityRepository, dimMemberHealthRepository);
    }

    @Test
    void shouldCalculateMemberHealth() {
        // Given
        UUID userId = UUID.randomUUID();
        String tenantId = "tenant-1";
        Long activityCount = 85L; // Should be CHAMPION, LOW risk

        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[] { userId, tenantId, activityCount });

        when(factActivityRepository.findUserActivityStats(any(LocalDateTime.class))).thenReturn(stats);

        // When
        job.calculateMemberHealth();

        // Then
        ArgumentCaptor<DimMemberHealth> healthCaptor = ArgumentCaptor.forClass(DimMemberHealth.class);
        verify(dimMemberHealthRepository).save(healthCaptor.capture());

        DimMemberHealth savedHealth = healthCaptor.getValue();
        assertThat(savedHealth.getUserId()).isEqualTo(userId);
        assertThat(savedHealth.getTenantId()).isEqualTo(tenantId);
        assertThat(savedHealth.getHealthScore()).isEqualTo(85);
        assertThat(savedHealth.getEngagementLevel()).isEqualTo("CHAMPION");
        assertThat(savedHealth.getChurnRisk()).isEqualTo("LOW");
    }

    @Test
    void shouldCalculateMemberHealthForLurker() {
        // Given
        UUID userId = UUID.randomUUID();
        String tenantId = "tenant-1";
        Long activityCount = 5L; // Should be LURKER, HIGH risk

        List<Object[]> stats = new ArrayList<>();
        stats.add(new Object[] { userId, tenantId, activityCount });

        when(factActivityRepository.findUserActivityStats(any(LocalDateTime.class))).thenReturn(stats);

        // When
        job.calculateMemberHealth();

        // Then
        ArgumentCaptor<DimMemberHealth> healthCaptor = ArgumentCaptor.forClass(DimMemberHealth.class);
        verify(dimMemberHealthRepository).save(healthCaptor.capture());

        DimMemberHealth savedHealth = healthCaptor.getValue();
        assertThat(savedHealth.getHealthScore()).isEqualTo(5);
        assertThat(savedHealth.getEngagementLevel()).isEqualTo("LURKER");
        assertThat(savedHealth.getChurnRisk()).isEqualTo("HIGH");
    }

    @Test
    void shouldHandleEmptyStats() {
        // Given
        when(factActivityRepository.findUserActivityStats(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        job.calculateMemberHealth();

        // Then
        verify(dimMemberHealthRepository, org.mockito.Mockito.never()).save(any(DimMemberHealth.class));
    }
}
