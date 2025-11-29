package com.openforum.datalake.ingestor.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReactionAdded(
        UUID reactionId,
        UUID targetId, // Could be Thread or Post ID
        String tenantId,
        UUID userId,
        String reactionType,
        LocalDateTime occurredAt) {
}
