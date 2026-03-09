package com.dispatchsim.controller;

import com.dispatchsim.model.dto.TaskCreateRequest;
import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.repository.TaskRepository;
import com.dispatchsim.service.TaskDispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*") // Frontend'in istek atabilmesi için CORS'u açıyoruz
@RequiredArgsConstructor
public class TaskController {

    private final TaskDispatcherService taskDispatcherService;
    private final TaskRepository taskRepository;

    // Yeni bir çağrı (Task) oluşturur ve kuyruğa atar
    @PostMapping
    public ResponseEntity<TaskEntity> createTask(@RequestBody TaskCreateRequest request) {
        TaskEntity newTask = new TaskEntity();
        newTask.setType(request.getType());
        newTask.setPriority(request.getPriority());
        
        TaskEntity submittedTask = taskDispatcherService.submitTask(newTask);
        return ResponseEntity.ok(submittedTask);
    }

    // Frontend ilk yüklendiğinde geçmiş task'leri ve durumları göstermek için
    @GetMapping
    public ResponseEntity<List<TaskEntity>> getAllTasks() {
        return ResponseEntity.ok(taskRepository.findAll());
    }
 // Tüm sistemi sıfırlar
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllTasks() {
        taskDispatcherService.clearAll();
        return ResponseEntity.ok().build();
    }
    @PostMapping("/deadlock")
    public ResponseEntity<Void> triggerDeadlock() {
        taskDispatcherService.triggerDeadlockSimulation();
        return ResponseEntity.ok().build();
    }
    @PostMapping("/starvation")
    public ResponseEntity<Void> triggerStarvation() {
        taskDispatcherService.triggerStarvationSimulation();
        return ResponseEntity.ok().build();
    }
    @PostMapping("/loom")
    public ResponseEntity<Void> triggerLoom(@RequestParam boolean virtual) {
        taskDispatcherService.triggerLoomSimulation(virtual);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/circuit-breaker")
    public ResponseEntity<Void> triggerCircuitBreaker() {
        taskDispatcherService.triggerCircuitBreakerSimulation();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/timeout")
    public ResponseEntity<Void> triggerTimeout() {
        taskDispatcherService.triggerTimeoutSimulation();
        return ResponseEntity.ok().build();
    }
}