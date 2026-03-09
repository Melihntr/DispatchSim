package com.dispatchsim.service;

import com.dispatchsim.model.dto.TaskEvent;
import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.model.enums.Priority;
import com.dispatchsim.model.enums.TaskStatus;
import com.dispatchsim.model.enums.TaskType;
import com.dispatchsim.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TaskDispatcherService {

    // DİKKAT: 'final' kelimesi KALDIRILDI. Çünkü havuzu silip yenisini atayacağız.
    private ThreadPoolExecutor dispatchExecutor;
    
    private final TaskRepository taskRepository;
    private final WebSocketPublisher webSocketPublisher;
    
    private final Object lockA = new Object();
    private final Object lockB = new Object();
    
    private int consecutiveFailures = 0;
    private boolean isCircuitOpen = false;
    private long circuitOpenTime = 0;
    private final long CIRCUIT_COOLDOWN_MS = 5000; // 5 saniye soğuma

    // Constructor'ı kendimiz tanımlıyoruz
    public TaskDispatcherService(ThreadPoolExecutor dispatchExecutor, TaskRepository taskRepository, WebSocketPublisher webSocketPublisher) {
        this.dispatchExecutor = dispatchExecutor;
        this.taskRepository = taskRepository;
        this.webSocketPublisher = webSocketPublisher;
    }
    
    public TaskEntity submitTask(TaskEntity task) {
        task.setStatus(TaskStatus.WAITING);
        TaskEntity savedTask = taskRepository.save(task);
        
        webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", savedTask));

        Runnable action = () -> {
            try {
                savedTask.setStatus(TaskStatus.RUNNING);
                savedTask.setStartedAt(LocalDateTime.now());
                taskRepository.save(savedTask); 
                
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
                
                webSocketPublisher.publishTaskUpdate(new TaskEvent(savedTask.getStatus().name(), savedTask));
            }
        };

        DispatchTask dispatchTask = new DispatchTask(savedTask, action);
        dispatchExecutor.execute(dispatchTask);

        return savedTask;
    }

    public void clearAll() {
        // 1. Eski kilitlenmiş havuzu zorla kapat
        dispatchExecutor.shutdownNow();
        
        // 2. Yepyeni, tertemiz bir thread havuzu yarat
        dispatchExecutor = new ThreadPoolExecutor(
                4, 8, 60L, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(100)
        );
        
        // 3. Veritabanındaki her şeyi sil
        taskRepository.deleteAll();
        
        // 4. Frontend'e "Ekranı Temizle" komutu gönder
        webSocketPublisher.publishTaskUpdate(new TaskEvent("CLEAR_ALL", null));
        
        log.info("Sistem ve Thread Havuzu tamamen sıfırlandı!");
    }

    public void triggerDeadlockSimulation() {
        log.warn("DEADLOCK SİMÜLASYONU BAŞLATILIYOR!");

        TaskEntity task1 = new TaskEntity();
        task1.setType(TaskType.CPU_BOUND);
        task1.setPriority(Priority.CRITICAL);
        task1.setStatus(TaskStatus.WAITING);
        taskRepository.save(task1);

        TaskEntity task2 = new TaskEntity();
        task2.setType(TaskType.CPU_BOUND);
        task2.setPriority(Priority.CRITICAL);
        task2.setStatus(TaskStatus.WAITING);
        taskRepository.save(task2);

        webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", task1));
        webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", task2));

        Runnable action1 = () -> {
            try {
                task1.setStatus(TaskStatus.RUNNING);
                taskRepository.save(task1);
                webSocketPublisher.publishTaskUpdate(new TaskEvent("RUNNING", task1));
                
                synchronized (lockA) { 
                    log.info("Task {} Lock A'yı aldı, B'yi bekliyor...", task1.getId());
                    Thread.sleep(500); 
                    
                    task1.setStatus(TaskStatus.BLOCKED);
                    taskRepository.save(task1);
                    webSocketPublisher.publishTaskUpdate(new TaskEvent("BLOCKED", task1));
                    
                    synchronized (lockB) { 
                        task1.setStatus(TaskStatus.SUCCESS);
                    }
                }
            } catch (Exception e) {}
        };

        Runnable action2 = () -> {
            try {
                task2.setStatus(TaskStatus.RUNNING);
                taskRepository.save(task2);
                webSocketPublisher.publishTaskUpdate(new TaskEvent("RUNNING", task2));
                
                synchronized (lockB) { 
                    log.info("Task {} Lock B'yi aldı, A'yı bekliyor...", task2.getId());
                    Thread.sleep(500); 
                    
                    task2.setStatus(TaskStatus.BLOCKED);
                    taskRepository.save(task2);
                    webSocketPublisher.publishTaskUpdate(new TaskEvent("BLOCKED", task2));
                    
                    synchronized (lockA) { 
                        task2.setStatus(TaskStatus.SUCCESS);
                    }
                }
            } catch (Exception e) {}
        };

        dispatchExecutor.execute(new DispatchTask(task1, action1));
        dispatchExecutor.execute(new DispatchTask(task2, action2));
    }
    public void triggerStarvationSimulation() {
        log.warn("STARVATION (AÇLIK) SİMÜLASYONU BAŞLATILIYOR!");

        // 1. Önce kurban LOW görevleri doldur (14 tane)
        for (int i = 0; i < 14; i++) {
            TaskEntity lowTask = new TaskEntity();
            lowTask.setType(TaskType.CPU_BOUND); // 3 saniye
            lowTask.setPriority(Priority.LOW);
            submitTask(lowTask);
        }

        // 2. CRITICAL görevleri ÇOK HIZLI pompala (Her 150ms'de bir)
        new Thread(() -> {
            // Thread'ler nefes alamadan 40 tane acil görev yollayacağız
            for (int i = 0; i < 40; i++) {
                try {
                    Thread.sleep(150); // 800ms'den 150ms'ye düşürdük! Hızlandırdık.
                    
                    TaskEntity criticalTask = new TaskEntity();
                    criticalTask.setType(TaskType.IO_BOUND); 
                    criticalTask.setPriority(Priority.CRITICAL);
                    
                    submitTask(criticalTask);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
    
    public void triggerLoomSimulation(boolean useVirtualThreads) {
        log.warn("LOOM SİMÜLASYONU BAŞLATILIYOR! Mod: " + (useVirtualThreads ? "VIRTUAL" : "PLATFORM"));

        // Mod seçimine göre anlık bir havuz yaratıyoruz
        java.util.concurrent.ExecutorService testExecutor = useVirtualThreads 
            ? java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor() // Sınırsız, hafif sanal thread'ler!
            : java.util.concurrent.Executors.newFixedThreadPool(4);            // Klasik 4 kapasiteli ağır thread'ler

        // Ekrana aynı anda 50 tane görev fırlatıyoruz (UI çökmesin diye 50'de tuttuk, backend milyonu da kaldırır)
        for (int i = 0; i < 50; i++) {
            TaskEntity task = new TaskEntity();
            task.setType(TaskType.IO_BOUND); 
            task.setPriority(Priority.MEDIUM);
            task.setStatus(TaskStatus.WAITING);
            TaskEntity savedTask = taskRepository.save(task);
            
            webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", savedTask));

            Runnable action = () -> {
                try {
                    savedTask.setStatus(TaskStatus.RUNNING);
                    taskRepository.save(savedTask);
                    webSocketPublisher.publishTaskUpdate(new TaskEvent("RUNNING", savedTask));
                    
                    // IO Bekleme Simülasyonu (2 Saniye)
                    Thread.sleep(2000); 

                    savedTask.setStatus(TaskStatus.SUCCESS);
                } catch (InterruptedException e) {
                    savedTask.setStatus(TaskStatus.FAILED);
                    Thread.currentThread().interrupt();
                } finally {
                    taskRepository.save(savedTask);
                    webSocketPublisher.publishTaskUpdate(new TaskEvent(savedTask.getStatus().name(), savedTask));
                }
            };

            testExecutor.execute(action);
        }
        
        // Klasik havuzsa işi bitince kapat (Sanal thread havuzunun kapatılmaya ihtiyacı yoktur)
        if (!useVirtualThreads) {
            testExecutor.shutdown();
        }
    }
    private synchronized boolean checkCircuitBreaker() {
        if (isCircuitOpen) {
            if (System.currentTimeMillis() - circuitOpenTime > CIRCUIT_COOLDOWN_MS) {
                log.info("Circuit Breaker: YARI AÇIK (Half-Open). Sistem test ediliyor...");
                isCircuitOpen = false; 
                consecutiveFailures = 0; 
                return true; // Test için 1 göreve izin ver
            }
            return false; // Hala kapalı, reddet
        }
        return true; 
    }

    private synchronized void recordSuccess() {
        consecutiveFailures = 0;
        isCircuitOpen = false;
    }

    private synchronized void recordFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= 3) { // 3 hatada şalter atar!
            isCircuitOpen = true;
            circuitOpenTime = System.currentTimeMillis();
            log.warn("ŞALTER ATTI! Sistem kendini korumaya aldı.");
        }
    }

    // 2. CIRCUIT BREAKER SİMÜLASYONU
    public void triggerCircuitBreakerSimulation() {
        new Thread(() -> {
            try {
                // Şalteri attırmak için peş peşe 3 HATALI görev yolluyoruz (FAILED olacak)
                for(int i=0; i<3; i++) {
                    submitSimulatedTask(true); 
                    Thread.sleep(800);
                }
                
                // Şalter attı! Şimdi yolladığımız NORMAL görevler anında reddedilecek (CANCELLED)
                for(int i=0; i<3; i++) {
                    submitSimulatedTask(false); 
                    Thread.sleep(500);
                }

                // Soğuma süresini bekle (Half-Open moduna geçiş)
                Thread.sleep(5000);

                // Şalter Yarı-Açık: Bir test görevi yolla (SUCCESS olacak)
                submitSimulatedTask(false);
                Thread.sleep(1500);

                // Sistem tamamen normale döndü (SUCCESS olacak)
                submitSimulatedTask(false);
            } catch (Exception e) {}
        }).start();
    }

    // Circuit Breaker'ın kullandığı özel arka plan metodu
    private void submitSimulatedTask(boolean willFail) {
        TaskEntity task = new TaskEntity();
        task.setType(TaskType.CPU_BOUND);
        task.setPriority(Priority.HIGH);
        task.setStatus(TaskStatus.WAITING);
        taskRepository.save(task);
        webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", task));

        // Şalter açıksa (OPEN) anında reddet! (CANCELLED)
        if (!checkCircuitBreaker()) {
            task.setStatus(TaskStatus.CANCELLED);
            taskRepository.save(task);
            webSocketPublisher.publishTaskUpdate(new TaskEvent("CANCELLED", task));
            return;
        }

        Runnable action = () -> {
            try {
                task.setStatus(TaskStatus.RUNNING);
                taskRepository.save(task);
                webSocketPublisher.publishTaskUpdate(new TaskEvent("RUNNING", task));
                Thread.sleep(1000); 
                
                if (willFail) throw new RuntimeException("Sistem Hatası!");
                
                recordSuccess();
                task.setStatus(TaskStatus.SUCCESS);
            } catch (Exception e) {
                recordFailure();
                task.setStatus(TaskStatus.FAILED);
            } finally {
                taskRepository.save(task);
                webSocketPublisher.publishTaskUpdate(new TaskEvent(task.getStatus().name(), task));
            }
        };
        dispatchExecutor.execute(action);
    }

    // 3. TIMEOUT SİMÜLASYONU
    public void triggerTimeoutSimulation() {
        TaskEntity task = new TaskEntity();
        task.setType(TaskType.IO_BOUND);
        task.setPriority(Priority.CRITICAL);
        task.setStatus(TaskStatus.WAITING);
        taskRepository.save(task);
        webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", task));

        Runnable action = () -> {
            try {
                task.setStatus(TaskStatus.RUNNING);
                taskRepository.save(task);
                webSocketPublisher.publishTaskUpdate(new TaskEvent("RUNNING", task));
                
                // 5 saniyelik uzun işlem (Ama bizim sınırımız 2sn olacak)
                Thread.sleep(5000); 
                task.setStatus(TaskStatus.SUCCESS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Watcher tarafından kesildi
            } finally {
                // Eğer Watcher iptal etmemişse SUCCESS kaydet, ettiyse TIMEOUT olarak kalır
                TaskEntity latestTask = taskRepository.findById(task.getId()).orElse(task);
                if (latestTask.getStatus() == TaskStatus.RUNNING) {
                    latestTask.setStatus(TaskStatus.SUCCESS);
                    taskRepository.save(latestTask);
                    webSocketPublisher.publishTaskUpdate(new TaskEvent("SUCCESS", latestTask));
                }
            }
        };
        
        // Görevi submit() ile yolluyoruz ki iptal etme hakkımız olsun (Future objesi)
        java.util.concurrent.Future<?> future = dispatchExecutor.submit(action);
        
        // Watcher (İzleyici) Thread
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 saniye bekle, görev hala bitmediyse kafasına sık!
                if (!future.isDone()) {
                    future.cancel(true); // Thread'i zorla kes (InterruptedException fırlatır)
                    task.setStatus(TaskStatus.TIMEOUT);
                    taskRepository.save(task);
                    webSocketPublisher.publishTaskUpdate(new TaskEvent("TIMEOUT", task));
                }
            } catch (Exception e) {}
        }).start();
    }
    
}