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
        // lockA = new Object();
        // lockB = new Object();
        // -------------------------

        isCircuitOpen = false;
        consecutiveFailures = 0;

        webSocketPublisher.publishTaskUpdate(new TaskEvent("CLEAR_ALL", null));
        log.info("Sistem, Kilitler ve Thread Havuzu tamamen sıfırlandı!");
    }

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

     // Hangi thread'in bu görevi aldığını tutacağımız referans
     java.util.concurrent.atomic.AtomicReference<Thread> executingThread = new java.util.concurrent.atomic.AtomicReference<>();

     Runnable action = () -> {
         executingThread.set(Thread.currentThread()); // Çalışan thread kendini belli ediyor
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

     // Artık submit() değil, kuyruğun sevdiği DispatchTask ile execute() yapıyoruz!
     dispatchExecutor.execute(new DispatchTask(task, action));

     // İzleyici (Watcher) Thread
     new Thread(() -> {
         try {
             Thread.sleep(2000);
             // Eğer görev hala RUNNING ise ve çalışan thread belliyse, onu zorla kes!
             if (task.getStatus() == TaskStatus.RUNNING && executingThread.get() != null) {
                 executingThread.get().interrupt();
             }
             // Eğer şalter vb. yüzünden havuza giremeden WAITING'de kaldıysa, direkt iptal et
             else if (task.getStatus() == TaskStatus.WAITING) {
                 task.setStatus(TaskStatus.TIMEOUT);
                 taskRepository.save(task);
                 webSocketPublisher.publishTaskUpdate(new TaskEvent("TIMEOUT", task));
             }
         } catch (Exception e) {}
     }).start();
 }
    public void triggerAutoScaleSimulation() {
        log.warn("AUTO-SCALE: Simülasyon başladı. Sisteme ani yük (Spike) bindiriliyor...");

        // 1. Sisteme aniden 20 tane uzun süren CPU görevi fırlatıyoruz (Kuyruk şişecek!)
        for (int i = 0; i < 20; i++) {
            TaskEntity task = new TaskEntity();
            task.setType(TaskType.CPU_BOUND); // Her biri 3 saniye sürer
            task.setPriority(Priority.MEDIUM);
            submitTask(task);
        }

        // 2. İzleyici (Watcher) Thread: Kuyruğu izleyip havuzu büyütecek ve küçültecek
        new Thread(() -> {
            try {
                // Sistemin zorlandığını fark etmesi için 2 saniye bekle
                Thread.sleep(2000);

                log.warn("AUTO-SCALE [SCALE OUT]: Yüksek yük tespit edildi! Havuz kapasitesi 4'ten 8'e çıkarılıyor.");
                // Önce Max'ı, sonra Core'u artırmalıyız (Java kuralı)
                dispatchExecutor.setMaximumPoolSize(8);
                dispatchExecutor.setCorePoolSize(8);

                // Sistem 8 thread ile tam gaz çalışırken metrikleri izleyebilmen için 10 saniye bekle
                Thread.sleep(10000);

                log.info("AUTO-SCALE [SCALE IN]: Yük azaldı, kaynak israfını önlemek için kapasite tekrar 4'e düşürülüyor.");
                // Önce Core'u, sonra Max'ı düşürmeliyiz (Java kuralı)
                dispatchExecutor.setCorePoolSize(4);
                dispatchExecutor.setMaximumPoolSize(4);

            } catch (Exception e) {}
        }).start();
    }
    // --- MEMORY LEAK & GC STRESS SİMÜLASYONU ---
    // --- GERÇEKÇİ MEMORY LEAK & GC STRESS SİMÜLASYONU ---
    public void triggerMemoryLeakSimulation() {
        log.warn("MEMORY LEAK SİMÜLASYONU BAŞLATILIYOR! Heap kapasitesi zorlanacak...");

        new Thread(() -> {
            java.util.List<byte[]> memoryLeakList = new java.util.ArrayList<>();
            try {
                // Sistemin tahsis ettiği Maksimum RAM miktarını öğren
                long maxMemory = Runtime.getRuntime().maxMemory();
                long targetMemory = (long) (maxMemory * 0.85); // Hedefimiz %85 doluluğa ulaşıp kırmızı barı görmek!

                log.info("Hedeflenen Doluluk: " + (targetMemory / (1024*1024)) + " MB");

                // JVM'i patlatmadan (OOM yemeden) %85'e kadar doldur
                for (int i = 0; i < 200; i++) {
                    long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

                    if (usedMemory >= targetMemory) {
                        log.warn("🚨 HEAP %85 DOLDU! Daha fazla doldurmuyoruz, sistemi izlemeye alıyoruz.");
                        break;
                    }

                    // Her adımda acımasızca 50 MB'lık (dev) bir obje yarat ve listeye at (Çöp toplayıcı silemesin)
                    memoryLeakList.add(new byte[50 * 1024 * 1024]);

                    // Frontend'deki metrik panelinin (1 sn'de bir yenilenen) çubuğun yükselişini yakalayabilmesi için biraz bekle
                    Thread.sleep(300);
                }

                // Ekrandaki o Kırmızı Barı ve GC çırpınışlarını izleyebilmen için sistemi 5 saniye bu eziyette tut!
                log.info("Sistem 5 saniye boyunca %85 yük altında tutuluyor...");
                Thread.sleep(5000);

            } catch (OutOfMemoryError e) {
                log.error("💥 OutOfMemoryError! Hafıza beklenenden hızlı patladı!");
            } catch (Exception e) {
            } finally {
                // Şov bitti, sistemi kurtar!
                memoryLeakList.clear(); // Referansları kopar
                System.gc(); // Çöp toplayıcıya "Hepsini temizle" emri ver
                log.info("Sistem hafızası boşaltıldı, tehlike geçti. Bar normale dönüyor.");
            }
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