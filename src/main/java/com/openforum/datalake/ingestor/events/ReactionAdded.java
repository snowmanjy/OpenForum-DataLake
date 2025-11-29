package com.openforum.datalake.ingestor.events;

import java.time.Instant;
import java.util.UUID;

public record ReactionAdded(
                UUID reactionId,
                UUID targetId, // Can be threadId or postId
                UUID userId,
                String reactionType, // LIKE, UPVOTE, etc.
                Instant occurredAt) {
}
