package com.pettrade.practiceplatform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SchedulerProperties {

    @Value("${app.scheduler.checkpoint-auto-save-ms:30000}")
    private long checkpointAutoSaveMs;

    public long getCheckpointAutoSaveMs() {
        return checkpointAutoSaveMs;
    }
}
