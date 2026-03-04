package com.dispatchsim.model.dto;

import com.dispatchsim.model.entity.TaskEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskEvent {
    private String eventType; // WAITING, RUNNING, SUCCESS, FAILED
    private TaskEntity task;
}
