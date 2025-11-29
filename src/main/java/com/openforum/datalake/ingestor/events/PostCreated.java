package com.openforum.datalake.ingestor.events;

import java.time.Instant;
import java.util.UUID;

public record PostCreated(
                UUID postId,
                UUID threadId,
                UUID authorId,
                String content,
                Instant createdAt) {
}
