package com.dispatchsim.service;

import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.model.enums.Priority;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DispatchTaskTest {

    @Test
    void testRunExecutesAction() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        TaskEntity taskEntity = new TaskEntity();
        
        DispatchTask task = new DispatchTask(taskEntity, () -> actionExecuted.set(true));
        task.run();

        // 1. Runnable blok çalıştı mı?
        assertTrue(actionExecuted.get());
        // 2. Getter çalışıyor mu?
        assertNotNull(task.getTaskEntity()); 
    }

    @Test
    void testCompareTo_PriorityRules() {
        TaskEntity criticalTask = new TaskEntity();
        criticalTask.setPriority(Priority.CRITICAL);

        TaskEntity lowTask = new TaskEntity();
        lowTask.setPriority(Priority.LOW);

        DispatchTask dtCritical = new DispatchTask(criticalTask, () -> {});
        DispatchTask dtLow = new DispatchTask(lowTask, () -> {});

        // CRITICAL, LOW'dan daha önde (küçük) olmalı
        assertTrue(dtCritical.compareTo(dtLow) < 0);
        assertTrue(dtLow.compareTo(dtCritical) > 0);
    }

    @Test
    void testCompareTo_SamePriority_UsesCreatedAt() {
        LocalDateTime now = LocalDateTime.now();
        
        TaskEntity oldTask = new TaskEntity();
        oldTask.setPriority(Priority.HIGH);
        oldTask.setCreatedAt(now.minusMinutes(5)); // 5 dakika eski

        TaskEntity newTask = new TaskEntity();
        newTask.setPriority(Priority.HIGH);
        newTask.setCreatedAt(now); // Yeni

        DispatchTask dtOld = new DispatchTask(oldTask, () -> {});
        DispatchTask dtNew = new DispatchTask(newTask, () -> {});

        // Öncelikler aynıysa, ESKİ olan (ilk gelen) daha önde olmalı
        assertTrue(dtOld.compareTo(dtNew) < 0);
        assertTrue(dtNew.compareTo(dtOld) > 0);
        assertEquals(0, dtOld.compareTo(dtOld)); // Kendisiyle kıyaslanırsa 0 dönmeli
    }
}
