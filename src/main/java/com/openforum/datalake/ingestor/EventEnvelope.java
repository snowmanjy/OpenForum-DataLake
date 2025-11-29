package com.openforum.datalake.ingestor;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record EventEnvelope(
        UUID eventId,
        String tenantId,
        String eventType,
        Instant occurredAt,
        JsonNode payload) {
}
