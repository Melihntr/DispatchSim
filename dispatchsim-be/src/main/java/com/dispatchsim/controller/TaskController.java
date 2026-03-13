package com.dispatchsim.controller;

import com.dispatchsim.model.dto.TaskCreateRequest;
import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.repository.TaskRepository;
import com.dispatchsim.service.TaskDispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Görev (Task) yönetimi ve sistem simülasyonları için REST API uç noktalarını sağlar.
 * Bu denetleyici, görev oluşturma, listeleme ve çeşitli JVM/Concurrency
 * senaryolarını tetikleme işlevlerini koordine eder.
 *
 * @author Melihntr
 * @version 1.0
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TaskController {

    private final TaskDispatcherService taskDispatcherService;
    private final TaskRepository taskRepository;

    /**
     * Yeni bir görev oluşturur ve işlenmek üzere kuyruğa ekler.
     * * @param request Görevin türünü ve önceliğini içeren talep nesnesi
     * @return Oluşturulan ve kuyruğa alınan görev nesnesi
     */
    @PostMapping
    public ResponseEntity<TaskEntity> createTask(@RequestBody TaskCreateRequest request) {
        TaskEntity newTask = new TaskEntity();
        newTask.setType(request.getType());
        newTask.setPriority(request.getPriority());

        TaskEntity submittedTask = taskDispatcherService.submitTask(newTask);
        return ResponseEntity.ok(submittedTask);
    }

    /**
     * Veritabanında kayıtlı olan tüm görevlerin listesini döner.
     * Genellikle frontend ilk yüklendiğinde geçmişi göstermek için kullanılır.
     * * @return Mevcut tüm görevlerin listesi
     */
    @GetMapping
    public ResponseEntity<List<TaskEntity>> getAllTasks() {
        return ResponseEntity.ok(taskRepository.findAll());
    }

    /**
     * Mevcut tüm görevleri veritabanından siler ve iş parçacığı havuzunu sıfırlar.
     * Sistemi temiz bir duruma getirmek için kullanılır.
     * * @return Boş yanıt (200 OK)
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearAllTasks() {
        taskDispatcherService.clearAll();
        return ResponseEntity.ok().build();
    }

    /**
     * İki iş parçacığının birbirini sonsuza kadar beklemesine neden olan
     * bir Deadlock (Ölümcül Kilitlenme) senaryosunu tetikler.
     */
    @PostMapping("/deadlock")
    public ResponseEntity<Void> triggerDeadlock() {
        taskDispatcherService.triggerDeadlockSimulation();
        return ResponseEntity.ok().build();
    }

    /**
     * Düşük öncelikli görevlerin kaynaklara erişemediği
     * Starvation (Açlık) senaryosunu tetikler.
     */
    @PostMapping("/starvation")
    public ResponseEntity<Void> triggerStarvation() {
        taskDispatcherService.triggerStarvationSimulation();
        return ResponseEntity.ok().build();
    }

    /**
     * Project Loom (Sanal İş Parçacıkları) performans testini başlatır.
     * * @param virtual true ise sanal thread'ler, false ise klasik thread'ler kullanılır.
     */
    @PostMapping("/loom")
    public ResponseEntity<Void> triggerLoom(@RequestParam boolean virtual) {
        taskDispatcherService.triggerLoomSimulation(virtual);
        return ResponseEntity.ok().build();
    }

    /**
     * Sistemin ardışık hatalar karşısında kendini korumaya aldığı
     * Devre Kesici (Circuit Breaker) simülasyonunu başlatır.
     */
    @PostMapping("/circuit-breaker")
    public ResponseEntity<Void> triggerCircuitBreaker() {
        taskDispatcherService.triggerCircuitBreakerSimulation();
        return ResponseEntity.ok().build();
    }

    /**
     * Belirli bir süreyi aşan görevlerin sistem tarafından
     * otomatik olarak sonlandırılmasını (Interrupt) simüle eder.
     */
    @PostMapping("/timeout")
    public ResponseEntity<Void> triggerTimeout() {
        taskDispatcherService.triggerTimeoutSimulation();
        return ResponseEntity.ok().build();
    }

    /**
     * Yoğun yük altında iş parçacığı havuzunun dinamik olarak büyümesini
     * ve yük azaldığında küçülmesini (Auto-Scaling) simüle eder.
     */
    @PostMapping("/autoscale")
    public ResponseEntity<Void> triggerAutoScale() {
        taskDispatcherService.triggerAutoScaleSimulation();
        return ResponseEntity.ok().build();
    }

    /**
     * Heap belleğin kasten doldurulduğu ve Garbage Collector (GC)
     * üzerindeki baskının izlendiği bellek sızıntısı senaryosunu başlatır.
     */
    @CrossOrigin
    @PostMapping("/memory-leak")
    public ResponseEntity<Void> triggerMemoryLeak() {
        taskDispatcherService.triggerMemoryLeakSimulation();
        return ResponseEntity.ok().build();
    }
}