package com.dispatchsim.controller;

import com.dispatchsim.model.dto.TaskCreateRequest;
import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.service.TaskDispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Görev (Task) yönetimi ve sistem simülasyonları için REST API uç noktalarını sağlar.
 * Bu denetleyici, istekleri alır ve işlenmek üzere TaskDispatcherService'e iletir.
 *
 * @author Melihntr
 * @version 1.1
 */
@RestController
@RequestMapping("/api/tasks")
// @CrossOrigin anotasyonunu sildik çünkü Global CorsConfig yazdık!
@RequiredArgsConstructor
public class TaskController {

    // SADECE Servis katmanıyla konuşuyoruz, Repository'ye erişim yasak!
    private final TaskDispatcherService taskDispatcherService;

    /**
     * Yeni bir görev oluşturur ve işlenmek üzere kuyruğa ekler.
     * @param request Görevin türünü ve önceliğini içeren talep nesnesi
     * @return Oluşturulan ve kuyruğa alınan görev nesnesi (201 Created)
     */
    @PostMapping
    public ResponseEntity<TaskEntity> createTask(@RequestBody TaskCreateRequest request) {
        TaskEntity newTask = new TaskEntity();
        newTask.setType(request.getType());
        newTask.setPriority(request.getPriority());

        TaskEntity submittedTask = taskDispatcherService.submitTask(newTask);
        return new ResponseEntity<>(submittedTask, HttpStatus.CREATED);
    }

    /**
     * Veritabanında kayıtlı olan tüm görevlerin listesini döner.
     * @return Mevcut tüm görevlerin listesi
     */
    @GetMapping
    public ResponseEntity<List<TaskEntity>> getAllTasks() {
        // Artık veriyi repodan değil, servisten istiyoruz!
        return ResponseEntity.ok(taskDispatcherService.getAllTasks());
    }

    /**
     * Mevcut tüm görevleri veritabanından siler ve iş parçacığı havuzunu sıfırlar.
     */
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

    @PostMapping("/autoscale")
    public ResponseEntity<Void> triggerAutoScale() {
        taskDispatcherService.triggerAutoScaleSimulation();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/memory-leak")
    public ResponseEntity<Void> triggerMemoryLeak() {
        taskDispatcherService.triggerMemoryLeakSimulation();
        return ResponseEntity.ok().build();
    }
}