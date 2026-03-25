package com.pettrade.practiceplatform.api.practice;

import java.time.LocalDateTime;

public record CheckpointResponse(Long checkpointId, String type, LocalDateTime createdAt, String snapshotJson) {
}
