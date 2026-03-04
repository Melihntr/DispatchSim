package com.dispatchsim.service;

import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.model.enums.TaskStatus;
import com.dispatchsim.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadPoolExecutor;

import com.dispatchsim.model.dto.TaskEvent;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskDispatcherService {

    private final ThreadPoolExecutor dispatchExecutor;
    private final TaskRepository taskRepository;
    private final WebSocketPublisher webSocketPublisher; // YENİ EKLENDİ

    public TaskEntity submitTask(TaskEntity task) {
        task.setStatus(TaskStatus.WAITING);
        TaskEntity savedTask = taskRepository.save(task);
        
        // Frontend'e haber ver: Yeni task kuyruğa girdi
        webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", savedTask));

        Runnable action = () -> {
            try {
                savedTask.setStatus(TaskStatus.RUNNING);
                savedTask.setStartedAt(LocalDateTime.now());
                taskRepository.save(savedTask); 
                
                // Frontend'e haber ver: Task thread havuzuna alındı ve çalışıyor
                webSocketPublisher.publishTaskUpdate(new TaskEvent("RUNNING", savedTask));

                long sleepTime = savedTask.getType().name().equals("CPU_BOUND") ? 3000 : 1500;
                Thread.sleep(sleepTime); 

                savedTask.setStatus(TaskStatus.SUCCESS);
            } catch (InterruptedException e) {
                savedTask.setStatus(TaskStatus.FAILED);
                Thread.currentThread().interrupt();
            } finally {
                savedTask.setFinishedAt(LocalDateTime.now());
                savedTask.setExecutionTimeMs(
                        java.time.Duration.between(savedTask.getStartedAt(), savedTask.getFinishedAt()).toMillis()
                );
                taskRepository.save(savedTask);
                
                // Frontend'e haber ver: Task bitti (Başarılı veya Hatalı)
                webSocketPublisher.publishTaskUpdate(new TaskEvent(savedTask.getStatus().name(), savedTask));
            }
        };

        DispatchTask dispatchTask = new DispatchTask(savedTask, action);
        dispatchExecutor.execute(dispatchTask);

        return savedTask;
    }
}