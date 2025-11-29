package com.openforum.datalake.ingestor.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubscriptionCreated(
        UUID subscriptionId,
        String tenantId,
        UUID userId,
        UUID targetId, // Thread ID usually
        LocalDateTime createdAt) {
}
