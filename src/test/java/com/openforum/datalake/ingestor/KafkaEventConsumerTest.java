package com.openforum.datalake.ingestor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openforum.datalake.domain.DimThread;
import com.openforum.datalake.domain.FactActivity;
import com.openforum.datalake.repository.DimThreadRepository;
import com.openforum.datalake.repository.FactActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaEventConsumerTest {

    @Mock
    private FactActivityRepository factActivityRepository;

    @Mock
    private DimThreadRepository dimThreadRepository;

    private KafkaEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        consumer = new KafkaEventConsumer(factActivityRepository, dimThreadRepository, objectMapper);
    }

    @Test
    void shouldProcessThreadCreatedEvent() throws Exception {
        // Given
        UUID threadId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String tenantId = "tenant-1";

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("threadId", threadId.toString());
        payload.put("categoryId", UUID.randomUUID().toString());
        payload.put("authorId", UUID.randomUUID().toString());
        payload.put("title", "Unit Test Thread");
        payload.put("createdAt", LocalDateTime.now().toString());

        EventEnvelope event = new EventEnvelope(
                eventId,
                "ThreadCreated",
                tenantId,
                threadId,
                LocalDateTime.now(),
                payload);
        String message = objectMapper.writeValueAsString(event);

        when(factActivityRepository.existsByEventId(eventId)).thenReturn(false);

        // When
        consumer.consume(message);

        // Then
        ArgumentCaptor<DimThread> threadCaptor = ArgumentCaptor.forClass(DimThread.class);
        verify(dimThreadRepository).save(threadCaptor.capture());
        assertThat(threadCaptor.getValue().getThreadId()).isEqualTo(threadId);
        assertThat(threadCaptor.getValue().getTitle()).isEqualTo("Unit Test Thread");

        ArgumentCaptor<FactActivity> factCaptor = ArgumentCaptor.forClass(FactActivity.class);
        verify(factActivityRepository).save(factCaptor.capture());
        assertThat(factCaptor.getValue().getEventId()).isEqualTo(eventId);
    }

    @Test
    void shouldProcessThreadImportedEvent() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        String tenantId = "tenant-1";
        LocalDateTime now = LocalDateTime.now();

        // Create a list of threads
        var threads = objectMapper.createArrayNode();

        ObjectNode thread1 = objectMapper.createObjectNode();
        UUID threadId1 = UUID.randomUUID();
        thread1.put("threadId", threadId1.toString());
        thread1.put("categoryId", UUID.randomUUID().toString());
        thread1.put("authorId", UUID.randomUUID().toString());
        thread1.put("title", "Imported Thread 1");
        thread1.put("createdAt", now.toString());
        threads.add(thread1);

        ObjectNode thread2 = objectMapper.createObjectNode();
        UUID threadId2 = UUID.randomUUID();
        thread2.put("threadId", threadId2.toString());
        thread2.put("categoryId", UUID.randomUUID().toString());
        thread2.put("authorId", UUID.randomUUID().toString());
        thread2.put("title", "Imported Thread 2");
        thread2.put("createdAt", now.toString());
        threads.add(thread2);

        EventEnvelope event = new EventEnvelope(
                eventId,
                "ThreadImported",
                tenantId,
                null, // Aggregate ID might be null for bulk import
                now,
                threads);
        String message = objectMapper.writeValueAsString(event);

        when(factActivityRepository.existsByEventId(eventId)).thenReturn(false);

        // When
        consumer.consume(message);

        // Then
        ArgumentCaptor<DimThread> threadCaptor = ArgumentCaptor.forClass(DimThread.class);
        verify(dimThreadRepository, org.mockito.Mockito.times(2)).save(threadCaptor.capture());

        assertThat(threadCaptor.getAllValues()).hasSize(2);
        assertThat(threadCaptor.getAllValues().get(0).getThreadId()).isEqualTo(threadId1);
        assertThat(threadCaptor.getAllValues().get(1).getThreadId()).isEqualTo(threadId2);

        ArgumentCaptor<FactActivity> factCaptor = ArgumentCaptor.forClass(FactActivity.class);
        verify(factActivityRepository, org.mockito.Mockito.times(2)).save(factCaptor.capture());

        assertThat(factCaptor.getAllValues()).hasSize(2);
        assertThat(factCaptor.getAllValues().get(0).getActivityType()).isEqualTo("THREAD_IMPORTED");
        assertThat(factCaptor.getAllValues().get(1).getActivityType()).isEqualTo("THREAD_IMPORTED");
    }
}
