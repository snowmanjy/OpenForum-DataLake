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

        ObjectNode thread = objectMapper.createObjectNode();
        UUID threadId = UUID.randomUUID();
        thread.put("threadId", threadId.toString());
        thread.put("categoryId", UUID.randomUUID().toString());
        thread.put("authorId", UUID.randomUUID().toString());
        thread.put("title", "Imported Thread");
        thread.put("createdAt", now.toString());

        EventEnvelope event = new EventEnvelope(
                eventId,
                "ThreadImported",
                tenantId,
                null,
                now,
                thread);
        String message = objectMapper.writeValueAsString(event);

        when(factActivityRepository.existsByEventId(eventId)).thenReturn(false);

        // When
        consumer.consume(message);

        // Then
        ArgumentCaptor<DimThread> threadCaptor = ArgumentCaptor.forClass(DimThread.class);
        verify(dimThreadRepository).save(threadCaptor.capture());
        assertThat(threadCaptor.getValue().getThreadId()).isEqualTo(threadId);

        ArgumentCaptor<FactActivity> factCaptor = ArgumentCaptor.forClass(FactActivity.class);
        verify(factActivityRepository).save(factCaptor.capture());
        assertThat(factCaptor.getValue().getActivityType()).isEqualTo("THREAD_IMPORTED");
    }

    @Test
    void shouldProcessPostImportedEvent() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        String tenantId = "tenant-1";
        LocalDateTime now = LocalDateTime.now();
        UUID threadId = UUID.randomUUID();

        ObjectNode post = objectMapper.createObjectNode();
        post.put("postId", UUID.randomUUID().toString());
        post.put("threadId", threadId.toString());
        post.put("authorId", UUID.randomUUID().toString());
        post.put("content", "Imported Post Content");
        post.put("createdAt", now.toString());

        EventEnvelope event = new EventEnvelope(
                eventId,
                "PostImported",
                tenantId,
                null,
                now,
                post);
        String message = objectMapper.writeValueAsString(event);

        when(factActivityRepository.existsByEventId(eventId)).thenReturn(false);
        // Mock DimThread lookup for update
        DimThread mockThread = new DimThread();
        mockThread.setThreadId(threadId);
        mockThread.setReplyCount(0);
        mockThread.setCreatedAt(now.minusHours(1));
        when(dimThreadRepository.findById(threadId)).thenReturn(java.util.Optional.of(mockThread));

        // When
        consumer.consume(message);

        // Then
        ArgumentCaptor<DimThread> threadCaptor = ArgumentCaptor.forClass(DimThread.class);
        verify(dimThreadRepository).save(threadCaptor.capture());
        assertThat(threadCaptor.getValue().getReplyCount()).isEqualTo(1);

        ArgumentCaptor<FactActivity> factCaptor = ArgumentCaptor.forClass(FactActivity.class);
        verify(factActivityRepository).save(factCaptor.capture());
        assertThat(factCaptor.getValue().getActivityType()).isEqualTo("POST_IMPORTED");
    }

    @Test
    void shouldProcessPostCreatedEvent() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        String tenantId = "tenant-1";
        LocalDateTime now = LocalDateTime.now();
        UUID threadId = UUID.randomUUID();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("threadId", threadId.toString());
        payload.put("postId", UUID.randomUUID().toString());
        payload.put("authorId", UUID.randomUUID().toString());
        payload.put("content", "Test Post");
        payload.put("createdAt", now.toString());

        EventEnvelope event = new EventEnvelope(
                eventId,
                "PostCreated",
                tenantId,
                null,
                now,
                payload);
        String message = objectMapper.writeValueAsString(event);

        when(factActivityRepository.existsByEventId(eventId)).thenReturn(false);

        DimThread mockThread = new DimThread();
        mockThread.setThreadId(threadId);
        mockThread.setReplyCount(0);
        mockThread.setCreatedAt(now.minusHours(1));
        when(dimThreadRepository.findById(threadId)).thenReturn(java.util.Optional.of(mockThread));

        // When
        consumer.consume(message);

        // Then
        ArgumentCaptor<DimThread> threadCaptor = ArgumentCaptor.forClass(DimThread.class);
        verify(dimThreadRepository).save(threadCaptor.capture());
        assertThat(threadCaptor.getValue().getReplyCount()).isEqualTo(1);

        ArgumentCaptor<FactActivity> factCaptor = ArgumentCaptor.forClass(FactActivity.class);
        verify(factActivityRepository).save(factCaptor.capture());
        assertThat(factCaptor.getValue().getActivityType()).isEqualTo("POST_CREATED");
    }

    @Test
    void shouldProcessReactionAddedEvent() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        String tenantId = "tenant-1";
        LocalDateTime now = LocalDateTime.now();
        UUID targetId = UUID.randomUUID();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("reactionId", UUID.randomUUID().toString());
        payload.put("targetId", targetId.toString());
        payload.put("reactorId", UUID.randomUUID().toString());
        payload.put("type", "LIKE");

        EventEnvelope event = new EventEnvelope(
                eventId,
                "ReactionAdded",
                tenantId,
                null,
                now,
                payload);
        String message = objectMapper.writeValueAsString(event);

        when(factActivityRepository.existsByEventId(eventId)).thenReturn(false);

        // When
        consumer.consume(message);

        // Then
        ArgumentCaptor<FactActivity> factCaptor = ArgumentCaptor.forClass(FactActivity.class);
        verify(factActivityRepository).save(factCaptor.capture());
        assertThat(factCaptor.getValue().getActivityType()).isEqualTo("REACTION");
        assertThat(factCaptor.getValue().getTargetId()).isEqualTo(targetId);
    }

    @Test
    void shouldProcessSubscriptionCreatedEvent() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        String tenantId = "tenant-1";
        LocalDateTime now = LocalDateTime.now();
        UUID targetId = UUID.randomUUID();

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("subscriptionId", UUID.randomUUID().toString());
        payload.put("targetId", targetId.toString());
        payload.put("subscriberId", UUID.randomUUID().toString());

        EventEnvelope event = new EventEnvelope(
                eventId,
                "SubscriptionCreated",
                tenantId,
                null,
                now,
                payload);
        String message = objectMapper.writeValueAsString(event);

        when(factActivityRepository.existsByEventId(eventId)).thenReturn(false);

        // When
        consumer.consume(message);

        // Then
        ArgumentCaptor<FactActivity> factCaptor = ArgumentCaptor.forClass(FactActivity.class);
        verify(factActivityRepository).save(factCaptor.capture());
        assertThat(factCaptor.getValue().getActivityType()).isEqualTo("SUBSCRIPTION_CREATED");
        assertThat(factCaptor.getValue().getTargetId()).isEqualTo(targetId);
    }

    @Test
    void shouldSkipDuplicateEvent() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        EventEnvelope event = new EventEnvelope(
                eventId,
                "ThreadCreated",
                "tenant-1",
                UUID.randomUUID(),
                LocalDateTime.now(),
                objectMapper.createObjectNode());
        String message = objectMapper.writeValueAsString(event);

        when(factActivityRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        consumer.consume(message);

        // Then
        verify(factActivityRepository, org.mockito.Mockito.never()).save(any(FactActivity.class));
    }

    @Test
    void shouldHandleJsonProcessingException() {
        // Given
        String invalidJson = "{invalid-json}";

        // When
        consumer.consume(invalidJson);

        // Then
        // No exception thrown, error logged
        verify(factActivityRepository, org.mockito.Mockito.never()).existsByEventId(any());
    }
}
