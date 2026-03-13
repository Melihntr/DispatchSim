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

/**
 * Uygulamanın merkezi görev dağıtım ve simülasyon servisidir.
 * Görevlerin kuyruğa alınması, öncelik yönetimi ve sistem hata senaryolarının
 * (Deadlock, Starvation, Circuit Breaker vb.) yürütülmesinden sorumludur.
 */
@Slf4j
@Service
public class TaskDispatcherService {

    private ThreadPoolExecutor dispatchExecutor;
    private final TaskRepository taskRepository;
    private final WebSocketPublisher webSocketPublisher;

    private int consecutiveFailures = 0;
    private boolean isCircuitOpen = false;
    private long circuitOpenTime = 0;
    private final long CIRCUIT_COOLDOWN_MS = 5000;

    /**
     * Gerekli bağımlılıklar ile servisi başlatır.
     */
    public TaskDispatcherService(ThreadPoolExecutor dispatchExecutor, TaskRepository taskRepository, WebSocketPublisher webSocketPublisher) {
        this.dispatchExecutor = dispatchExecutor;
        this.taskRepository = taskRepository;
        this.webSocketPublisher = webSocketPublisher;
    }

    /**
     * Yeni bir görevi sisteme kabul eder ve iş parçacığı havuzunda yürütülmek üzere kuyruğa alır.
     */
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

    /**
     * Mevcut tüm işleri durdurur, veritabanını temizler ve thread havuzunu sıfırlar.
     */
    public void clearAll() {
        dispatchExecutor.shutdownNow();

        dispatchExecutor = new ThreadPoolExecutor(
                4, 8, 60L, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(100)
        );

        taskRepository.deleteAll();

        isCircuitOpen = false;
        consecutiveFailures = 0;

        webSocketPublisher.publishTaskUpdate(new TaskEvent("CLEAR_ALL", null));
        log.info("Sistem, Kilitler ve Thread Havuzu tamamen sıfırlandı!");
    }

    /**
     * İki iş parçacığının birbirini beklemesiyle oluşan Deadlock durumunu simüle eder.
     */
    public void triggerDeadlockSimulation() {
        log.warn("DEADLOCK SİMÜLASYONU BAŞLATILIYOR!");

        Object lockA = new Object();
        Object lockB = new Object();

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

    /**
     * Düşük öncelikli görevlerin kaynak bulamadığı Starvation durumunu simüle eder.
     */
    public void triggerStarvationSimulation() {
        log.warn("STARVATION (AÇLIK) SİMÜLASYONU BAŞLATILIYOR!");

        for (int i = 0; i < 14; i++) {
            TaskEntity lowTask = new TaskEntity();
            lowTask.setType(TaskType.CPU_BOUND);
            lowTask.setPriority(Priority.LOW);
            submitTask(lowTask);
        }

        new Thread(() -> {
            for (int i = 0; i < 40; i++) {
                try {
                    Thread.sleep(150);

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

    /**
     * Sanal iş parçacıkları (Loom) ile klasik thread havuzu performansını karşılaştırır.
     */
    public void triggerLoomSimulation(boolean useVirtualThreads) {
        log.warn("LOOM SİMÜLASYONU BAŞLATILIYOR! Mod: " + (useVirtualThreads ? "VIRTUAL" : "PLATFORM"));

        java.util.concurrent.ExecutorService testExecutor = useVirtualThreads
                ? java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()
                : java.util.concurrent.Executors.newFixedThreadPool(4);

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
                return true;
            }
            return false;
        }
        return true;
    }

    private synchronized void recordSuccess() {
        consecutiveFailures = 0;
        isCircuitOpen = false;
    }

    private synchronized void recordFailure() {
        consecutiveFailures++;
        if (consecutiveFailures >= 3) {
            isCircuitOpen = true;
            circuitOpenTime = System.currentTimeMillis();
            log.warn("ŞALTER ATTI! Sistem kendini korumaya aldı.");
        }
    }

    /**
     * Hata eşiği aşıldığında sistemin kendini korumaya almasını simüle eder.
     */
    public void triggerCircuitBreakerSimulation() {
        new Thread(() -> {
            try {
                log.warn("CIRCUIT BREAKER: 3 Hatalı Görev Yollanıyor...");
                for(int i = 0; i < 3; i++) {
                    submitSimulatedTask(true);
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
        DispatchTask dispatchTask = new DispatchTask(task, action);
        dispatchExecutor.execute(dispatchTask);
    }

    /**
     * Zaman aşımına uğrayan görevlerin sistem tarafından sonlandırılmasını simüle eder.
     */
    public void triggerTimeoutSimulation() {
        TaskEntity task = new TaskEntity();
        task.setType(TaskType.IO_BOUND);
        task.setPriority(Priority.CRITICAL);
        task.setStatus(TaskStatus.WAITING);
        taskRepository.save(task);
        webSocketPublisher.publishTaskUpdate(new TaskEvent("WAITING", task));

        java.util.concurrent.atomic.AtomicReference<Thread> executingThread = new java.util.concurrent.atomic.AtomicReference<>();

        Runnable action = () -> {
            executingThread.set(Thread.currentThread());
            try {
                task.setStatus(TaskStatus.RUNNING);
                taskRepository.save(task);
                webSocketPublisher.publishTaskUpdate(new TaskEvent("RUNNING", task));

                Thread.sleep(5000);
                task.setStatus(TaskStatus.SUCCESS);
            } catch (InterruptedException e) {
                task.setStatus(TaskStatus.TIMEOUT);
                Thread.currentThread().interrupt();
            } finally {
                taskRepository.save(task);
                webSocketPublisher.publishTaskUpdate(new TaskEvent(task.getStatus().name(), task));
            }
        };

        dispatchExecutor.execute(new DispatchTask(task, action));

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                if (task.getStatus() == TaskStatus.RUNNING && executingThread.get() != null) {
                    executingThread.get().interrupt();
                }
                else if (task.getStatus() == TaskStatus.WAITING) {
                    task.setStatus(TaskStatus.TIMEOUT);
                    taskRepository.save(task);
                    webSocketPublisher.publishTaskUpdate(new TaskEvent("TIMEOUT", task));
                }
            } catch (Exception e) {}
        }).start();
    }

    /**
     * Yoğun yük altında havuz kapasitesinin otomatik artışını ve azalışını simüle eder.
     */
    public void triggerAutoScaleSimulation() {
        log.warn("AUTO-SCALE: Simülasyon başladı. Sisteme ani yük (Spike) bindiriliyor...");

        for (int i = 0; i < 20; i++) {
            TaskEntity task = new TaskEntity();
            task.setType(TaskType.CPU_BOUND);
            task.setPriority(Priority.MEDIUM);
            submitTask(task);
        }

        new Thread(() -> {
            try {
                Thread.sleep(2000);

                log.warn("AUTO-SCALE [SCALE OUT]: Yüksek yük tespit edildi! Havuz kapasitesi 4'ten 8'e çıkarılıyor.");
                dispatchExecutor.setMaximumPoolSize(8);
                dispatchExecutor.setCorePoolSize(8);

                Thread.sleep(10000);

                log.info("AUTO-SCALE [SCALE IN]: Yük azaldı, kaynak israfını önlemek için kapasite tekrar 4'e düşürülüyor.");
                dispatchExecutor.setCorePoolSize(4);
                dispatchExecutor.setMaximumPoolSize(4);

            } catch (Exception e) {}
        }).start();
    }

    /**
     * JVM Heap bellek doluluğunun arttığı ve GC baskısının oluştuğu durumu simüle eder.
     */
    public void triggerMemoryLeakSimulation() {
        log.warn("MEMORY LEAK SİMÜLASYONU BAŞLATILIYOR! Heap kapasitesi zorlanacak...");

        new Thread(() -> {
            java.util.List<byte[]> memoryLeakList = new java.util.ArrayList<>();
            try {
                long maxMemory = Runtime.getRuntime().maxMemory();
                long targetMemory = (long) (maxMemory * 0.85);

                log.info("Hedeflenen Doluluk: " + (targetMemory / (1024*1024)) + " MB");

                for (int i = 0; i < 200; i++) {
                    long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

                    if (usedMemory >= targetMemory) {
                        log.warn("HEAP %85 DOLDU! Daha fazla doldurmuyoruz, sistemi izlemeye alıyoruz.");
                        break;
                    }

                    memoryLeakList.add(new byte[50 * 1024 * 1024]);
                    Thread.sleep(300);
                }

                log.info("Sistem 5 saniye boyunca %85 yük altında tutuluyor...");
                Thread.sleep(5000);

            } catch (OutOfMemoryError e) {
                log.error(" OutOfMemoryError! Hafıza beklenenden hızlı patladı!");
            } catch (Exception e) {
                log.error("Bellek simülasyonu kesildi: {}", e.getMessage());
            } finally {
                memoryLeakList.clear();
                System.gc();
                log.info("Sistem hafızası boşaltıldı, tehlike geçti. Bar normale dönüyor.");
            }
        }).start();
    }

    /** @return Aktif çalışan thread sayısı */
    public int getActiveThreadCount() {
        return dispatchExecutor.getActiveCount();
    }

    /** @return Kuyrukta bekleyen iş sayısı */
    public int getQueueSize() {
        return dispatchExecutor.getQueue().size();
    }

    /** @return Havuzun maksimum thread kapasitesi */
    public int getMaxThreads() {
        return dispatchExecutor.getMaximumPoolSize();
    }
}