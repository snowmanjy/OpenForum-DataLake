package com.openforum.datalake.ingestor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.datalake.domain.DimThread;
import com.openforum.datalake.repository.DimThreadRepository;
import com.openforum.datalake.domain.FactActivity;
import com.openforum.datalake.repository.FactActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Component
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final FactActivityRepository factActivityRepository;
    private final DimThreadRepository dimThreadRepository;
    private final ObjectMapper objectMapper;

    public KafkaEventConsumer(FactActivityRepository factActivityRepository, DimThreadRepository dimThreadRepository,
            ObjectMapper objectMapper) {
        this.factActivityRepository = factActivityRepository;
        this.dimThreadRepository = dimThreadRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "forum-events-v1", groupId = "datalake-consumer-group")
    @Transactional
    public void consume(String message) {
        System.out.println("Received message: " + message);
        try {
            EventEnvelope event = objectMapper.readValue(message, EventEnvelope.class);
            log.info("Received event: {} type: {}", event.eventId(), event.type());

            if (factActivityRepository.existsByEventId(event.eventId())) {
                log.info("Event {} already processed. Skipping.", event.eventId());
                return;
            }

            processEvent(event);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse event", e);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error processing event", e);
        }
    }

    private void processEvent(EventEnvelope event) {
        try {
            switch (event.type()) {
                case "ThreadCreated":
                    handleThreadCreated(event);
                    break;
                case "PostCreated":
                    handlePostCreated(event);
                    break;
                case "ReactionAdded":
                    handleReactionAdded(event);
                    break;
                case "SubscriptionCreated":
                    handleSubscriptionCreated(event);
                    break;
                case "ThreadImported":
                    handleThreadImported(event);
                    break;
                default:
                    log.info("Ignored event type: {}", event.type());
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("Idempotency check failed (Duplicate): {}", event.eventId());
        }
    }

    private void handleThreadCreated(EventEnvelope event) {
        // Use static factory method for mapping
        DimThread thread = DimThread.from(event);
        dimThreadRepository.save(thread);

        // Use static factory method for mapping
        FactActivity fact = FactActivity.from(event, "THREAD_CREATED", thread.getThreadId());
        factActivityRepository.save(fact);
    }

    private void handlePostCreated(EventEnvelope event) {
        JsonNode payload = event.payload();
        UUID threadId = UUID.fromString(payload.get("threadId").asText());

        // Update DimThread
        dimThreadRepository.findById(threadId).ifPresent(thread -> {
            thread.setLastActivityAt(event.occurredAt());
            thread.setReplyCount(thread.getReplyCount() + 1);

            // Calculate response time if first reply
            if (thread.getReplyCount() == 1) {
                long diff = Duration.between(thread.getCreatedAt(), event.occurredAt()).toMinutes();
                thread.setResponseTimeMinutes((int) diff);
            }

            dimThreadRepository.save(thread);
        });

        // Use static factory method for mapping
        FactActivity fact = FactActivity.from(event, "POST_CREATED", threadId);
        factActivityRepository.save(fact);
    }

    private void handleReactionAdded(EventEnvelope event) {
        UUID targetId = UUID.fromString(event.payload().get("targetId").asText());
        FactActivity fact = FactActivity.from(event, "REACTION", targetId);
        factActivityRepository.save(fact);
    }

    private void handleSubscriptionCreated(EventEnvelope event) {
        UUID targetId = UUID.fromString(event.payload().get("targetId").asText());
        FactActivity fact = FactActivity.from(event, "SUBSCRIPTION_CREATED", targetId);
        factActivityRepository.save(fact);
    }

    private void handleThreadImported(EventEnvelope event) {
        JsonNode payload = event.payload();
        if (payload.isArray()) {
            for (JsonNode threadNode : payload) {
                // Create DimThread
                DimThread thread = DimThread.from(threadNode, event.tenantId(), event.occurredAt());
                dimThreadRepository.save(thread);

                // Create FactActivity for each thread
                // We need to create a unique event ID for each fact if we want to track them
                // individually,
                // but FactActivity.from uses the envelope's eventId.
                // Since this is one event (Import) resulting in multiple facts, we might have a
                // PK conflict if we use the same eventId for all.
                // FactActivityId is (eventId, occurredAt).
                // If we insert multiple rows with same eventId, we violate PK.
                // However, FactActivity represents the *Event*.
                // If one event caused multiple things, maybe we should only have ONE
                // FactActivity for the "Import" event?
                // OR, we treat each imported thread as a separate activity?
                // The user request says: "insert a DimThread and FactActivity for each item".
                // This implies we need distinct FactActivities.
                // We can generate a derived eventId or use the threadId as part of the key?
                // FactActivityId is (eventId, occurredAt).
                // If we strictly follow "one FactActivity per item", we need unique eventIds.
                // Let's generate a new UUID for the FactActivity to avoid PK collision,
                // effectively treating the batch item as a sub-event.

                UUID subEventId = UUID.randomUUID();
                FactActivity fact = new FactActivity();
                fact.setId(
                        new com.openforum.datalake.domain.FactActivity.FactActivityId(subEventId, event.occurredAt()));
                fact.setEventId(subEventId);
                fact.setActivityType("THREAD_IMPORTED");
                fact.setTenantId(event.tenantId());
                fact.setTargetId(thread.getThreadId());
                fact.setMetadata(objectMapper.convertValue(threadNode, java.util.Map.class));

                factActivityRepository.save(fact);
            }
        }
    }
}
