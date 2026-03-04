package com.dispatchsim.model.dto;

import com.dispatchsim.model.enums.Priority;
import com.dispatchsim.model.enums.TaskType;
import lombok.Data;

@Data
public class TaskCreateRequest {
    private TaskType type;         // CPU_BOUND veya IO_BOUND
    private Priority priority;     // LOW, MEDIUM, HIGH, CRITICAL
}