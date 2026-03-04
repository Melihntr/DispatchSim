package com.dispatchsim.model.entity;

import com.dispatchsim.model.enums.Priority;
import com.dispatchsim.model.enums.TaskStatus;
import com.dispatchsim.model.enums.TaskType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private Long executionTimeMs;

    // Görev bittiğindeki tahmini bellek snapshot'ı
    private Long memoryAllocatedSnapshot; 

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TaskStatus.WAITING;
        }
    }
}
