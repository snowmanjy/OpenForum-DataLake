package com.openforum.datalake.ingestor.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostCreated(
        UUID postId,
        UUID threadId,
        String tenantId,
        UUID authorId,
        String content,
        LocalDateTime createdAt) {
}
