package com.pettrade.practiceplatform.service.checkpoint;

import com.pettrade.practiceplatform.domain.enumtype.CheckpointType;

public final class CheckpointTypeResolver {

    private CheckpointTypeResolver() {
    }

    public static CheckpointType fromReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return CheckpointType.MANUAL;
        }
        try {
            return CheckpointType.valueOf(reason.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CheckpointType.MANUAL;
        }
    }
}
