package com.openforum.datalake.ingestor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        UUID eventId,
        String type,
        String tenantId,
        UUID aggregateId,
        LocalDateTime occurredAt,
        JsonNode payload) {
}
