package com.dispatchsim.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemMetrics {
    private double heapUsedMb;
    private double heapMaxMb;
    private long gcPauseCount;
    private double gcPauseTotalTimeMs;
    private int activeThreads;
    private int queuedTasks;
}
