package com.openforum.datalake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.datalake.repository.DimThreadRepository;
import com.openforum.datalake.repository.FactActivityRepository;
import com.openforum.datalake.ingestor.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class DataLakeIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DimThreadRepository dimThreadRepository;

    @Autowired
    private FactActivityRepository factActivityRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public org.apache.kafka.clients.admin.NewTopic topic() {
            return org.springframework.kafka.config.TopicBuilder.name("forum-events-v1").partitions(1).replicas(1)
                    .build();
        }
    }

    @Test
    void testEndToEndPipeline() throws Exception {
        UUID threadId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String tenantId = "tenant-1";
        LocalDateTime now = LocalDateTime.now();

        // 1. Send ThreadCreated Event
        Map<String, Object> threadPayload = Map.of(
                "threadId", threadId.toString(),
                "categoryId", categoryId.toString(),
                "authorId", authorId.toString(),
                "title", "Test Thread",
                "createdAt", now.toString(),
                "tags", Collections.singletonList("java"));

        EventEnvelope threadCreated = new EventEnvelope(
                UUID.randomUUID(),
                "ThreadCreated",
                tenantId,
                threadId,
                now,
                objectMapper.valueToTree(threadPayload));

        kafkaTemplate.send("forum-events-v1", objectMapper.writeValueAsString(threadCreated));

        // Wait for DimThread
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(dimThreadRepository.findById(threadId)).isPresent();
            assertThat(factActivityRepository.count()).isEqualTo(1);
        });

        // 2. Send PostCreated Event
        Map<String, Object> postPayload = Map.of(
                "threadId", threadId.toString(),
                "postId", UUID.randomUUID().toString(),
                "authorId", authorId.toString());

        EventEnvelope postCreated = new EventEnvelope(
                UUID.randomUUID(),
                "PostCreated",
                tenantId,
                authorId,
                now.plusMinutes(5),
                objectMapper.valueToTree(postPayload));

        kafkaTemplate.send("forum-events-v1", objectMapper.writeValueAsString(postCreated));

        // Wait for Update
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var thread = dimThreadRepository.findById(threadId).get();
            assertThat(thread.getReplyCount()).isEqualTo(1);
            assertThat(factActivityRepository.count()).isEqualTo(2);
        });
    }
}
