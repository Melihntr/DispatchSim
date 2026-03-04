package com.dispatchsim.service;

import com.dispatchsim.model.entity.TaskEntity;
import lombok.Getter;

@Getter
public class DispatchTask implements Runnable, Comparable<DispatchTask> {

    private final TaskEntity taskEntity;
    private final Runnable action; // CPU veya IO simülasyonunu yapacak asıl iş bloğu

    public DispatchTask(TaskEntity taskEntity, Runnable action) {
        this.taskEntity = taskEntity;
        this.action = action;
    }

    @Override
    public void run() {
        // Thread bu task'i aldığında çalıştırılacak blok
        action.run();
    }

    @Override
    public int compareTo(DispatchTask other) {
        // 1. Kural: Önceliği yüksek olan (CRITICAL > HIGH > MEDIUM > LOW) öne geçer.
        // Enum'da aşağıda olanın ordinal değeri daha yüksektir, bu yüzden tersten karşılaştırıyoruz.
        int priorityCompare = other.taskEntity.getPriority().compareTo(this.taskEntity.getPriority());
        
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        
        // 2. Kural: Eğer öncelikler aynıysa, sisteme ilk gelen (createdAt) öne geçer (FIFO).
        return this.taskEntity.getCreatedAt().compareTo(other.taskEntity.getCreatedAt());
    }
}
