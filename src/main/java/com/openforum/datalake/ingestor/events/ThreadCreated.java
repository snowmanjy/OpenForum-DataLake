package com.openforum.datalake.ingestor.events;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ThreadCreated(
        UUID threadId,
        String tenantId,
        UUID categoryId,
        UUID authorId,
        String title,
        List<String> tags,
        LocalDateTime createdAt) {
}
