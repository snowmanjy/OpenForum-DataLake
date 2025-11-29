package com.openforum.datalake.ingestor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openforum.datalake.repository.DimThreadRepository;
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
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class KafkaImportIngestionTest {

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
    void shouldIngestImportedThread() throws Exception {
        String tenantId = "tenant-import-1";
        Instant now = Instant.now();
        UUID eventId = UUID.randomUUID();

        ObjectNode thread = objectMapper.createObjectNode();
        thread.put("threadId", UUID.randomUUID().toString());
        thread.put("categoryId", UUID.randomUUID().toString());
        thread.put("authorId", UUID.randomUUID().toString());
        thread.put("title", "Imported Thread Single");
        thread.put("createdAt", now.toString());

        EventEnvelope event = new EventEnvelope(
                eventId,
                tenantId,
                "ThreadImported",
                now,
                thread);

        kafkaTemplate.send("forum-events-v1", objectMapper.writeValueAsString(event));

        // Wait for DimThread count to increase by 1
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            long count = dimThreadRepository.countTotalThreads(tenantId);
            assertThat(count).isEqualTo(1);
        });
    }
}
