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
    
    private Object lockA = new Object();
    private Object lockB = new Object();
    
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
        dispatchExecutor.shutdownNow();
        
        dispatchExecutor = new ThreadPoolExecutor(
                4, 8, 60L, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(100)
        );
        
        taskRepository.deleteAll();
        
        // --- YENİ EKLENEN KISIM ---
        // Zombilerin elindeki zehirli kilitleri çöpe atıp, sisteme yeni kilitler veriyoruz!
        lockA = new Object();
        lockB = new Object();
        // -------------------------

        isCircuitOpen = false;
        consecutiveFailures = 0;

        webSocketPublisher.publishTaskUpdate(new TaskEvent("CLEAR_ALL", null));
        log.info("Sistem, Kilitler ve Thread Havuzu tamamen sıfırlandı!");
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

 // --------------------------------------------------
    // 2. KUSURSUZ ZAMANLAMALI CIRCUIT BREAKER SİMÜLASYONU
    // --------------------------------------------------
    public void triggerCircuitBreakerSimulation() {
        new Thread(() -> {
            try {
                log.warn("CIRCUIT BREAKER: 3 Hatalı Görev Yollanıyor...");
                for(int i = 0; i < 3; i++) {
                    submitSimulatedTask(true); 
                    // DÜZELTME: Görev 1 saniye sürüyor. 1.2 sn bekliyoruz ki patladığından emin olalım!
                    Thread.sleep(1200); 
                }
                
                log.warn("CIRCUIT BREAKER: Şalter Attı! Yeni görevler reddedilecek.");
                for(int i = 0; i < 3; i++) {
                    submitSimulatedTask(false); 
                    Thread.sleep(800);
                }

                log.info("CIRCUIT BREAKER: 5 saniye soğuma (cooldown) bekleniyor...");
                Thread.sleep(5000);

                log.info("CIRCUIT BREAKER: Half-Open (Yarı Açık) test görevi...");
                submitSimulatedTask(false);
                Thread.sleep(1500);

                log.info("CIRCUIT BREAKER: Sistem normale döndü, son görev...");
                submitSimulatedTask(false);
            } catch (Exception e) {}
        }).start();
    }

    // Circuit Breaker'ın Arka Plan Metodu
    private void submitSimulatedTask(boolean willFail) {
        TaskEntity task = new TaskEntity();
        task.setType(TaskType.CPU_BOUND);
        task.setPriority(Priority.HIGH);
        task.setStatus(TaskStatus.WAITING);
        taskRepository.save(task);
        webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", task));

        if (!checkCircuitBreaker()) {
            task.setStatus(TaskStatus.CANCELLED);
            taskRepository.save(task);
            webSocketPublisher.publishTaskUpdate(new TaskEvent("CANCELLED", task));
            return; // Görev iptal edildi, havuza hiç GİRMESİN.
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
        DispatchTask dispatchTask = new DispatchTask(task, action);
        dispatchExecutor.execute(dispatchTask);
    }

 // --------------------------------------------------
    // 1. ZIRHLANDIRILMIŞ TIMEOUT SİMÜLASYONU
    // --------------------------------------------------
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
                
                Thread.sleep(5000); // 5 saniyelik uzun işlem
                
                // Eğer kesilmeden buraya ulaştıysa, SUCCESS'tir
                task.setStatus(TaskStatus.SUCCESS);
            } catch (InterruptedException e) {
                // İzleyici (Watcher) tarafından Future iptal edilirse buraya düşer
                task.setStatus(TaskStatus.TIMEOUT);
                Thread.currentThread().interrupt();
            } finally {
                // Veritabanından okuma yapmadan, doğrudan elindeki net durumu kaydet!
                taskRepository.save(task);
                webSocketPublisher.publishTaskUpdate(new TaskEvent(task.getStatus().name(), task));
            }
        };
        
        java.util.concurrent.Future<?> future = dispatchExecutor.submit(action);
        
        // Watcher (İzleyici) Thread
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 saniye bekle
                if (!future.isDone()) {
                    future.cancel(true); // Görevin kafasına sık (InterruptedException fırlatır)
                    
                    // ÖNEMLİ KONTROL: Eğer görev havuza giremeden kuyrukta beklerken iptal edildiyse:
                    if (task.getStatus() == TaskStatus.WAITING) {
                        task.setStatus(TaskStatus.TIMEOUT);
                        taskRepository.save(task);
                        webSocketPublisher.publishTaskUpdate(new TaskEvent("TIMEOUT", task));
                    }
                }
            } catch (Exception e) {}
        }).start();
    }
    public int getActiveThreadCount() {
        return dispatchExecutor.getActiveCount();
    }
    public int getQueueSize() {
        return dispatchExecutor.getQueue().size();
    }
    public int getMaxThreads() {
        return dispatchExecutor.getMaximumPoolSize();
    }
    
}