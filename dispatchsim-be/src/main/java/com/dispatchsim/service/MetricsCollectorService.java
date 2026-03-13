package com.dispatchsim.service;

import com.dispatchsim.model.dto.SystemMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.TimeUnit;

/**
 * JVM metriklerini ve iş parçacığı havuzu durumunu periyodik olarak toplayan
 * ve WebSocket üzerinden yayınlayan servis sınıfıdır.
 * <p>
 * Bu servis, uygulamanın anlık sağlık durumunu (Memory kullanımı, GC duraklamaları)
 * ve {@link TaskDispatcherService} üzerindeki iş yükünü (aktif threadler, bekleyen görevler)
 * izleyerek görselleştirme için veri sağlar.
 * </p>
 *
 * @author Melihntr
 */
@Service
@RequiredArgsConstructor
public class MetricsCollectorService {

    private final MeterRegistry meterRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskDispatcherService taskDispatcherService;

    /**
     * Her 1000 milisaniyede (1 saniye) bir çalışarak güncel sistem metriklerini toplar.
     * <p>
     * Toplanan veriler şunları içerir:
     * <ul>
     * <li><b>Bellek:</b> JVM Heap kullanımı ve maksimum kapasitesi (MB cinsinden).</li>
     * <li><b>Garbage Collection:</b> Toplam GC duraklama sayısı ve süresi.</li>
     * <li><b>Havuz Durumu:</b> Aktif çalışan thread sayısı, kuyrukta bekleyen görevler ve havuz limiti.</li>
     * </ul>
     * Veriler toplandıktan sonra {@code /topic/metrics} kanalına {@link SystemMetrics} DTO'su olarak gönderilir.
     * </p>
     */
    @Scheduled(fixedRate = 1000)
    public void publishMetrics() {
        try {
            // JVM Management Factory üzerinden bellek bilgilerini oku
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            double heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024.0 * 1024.0);
            double heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024.0 * 1024.0);

            // GC Duraklama (Pause) metriklerini Micrometer üzerinden çek
            long gcCount = 0;
            double gcTotalTime = 0;
            try {
                // Not: "jvm.gc.pause" metriği Micrometer tarafından otomatik sağlanır
                gcCount = (long) meterRegistry.get("jvm.gc.pause").timer().count();
                gcTotalTime = meterRegistry.get("jvm.gc.pause").timer().totalTime(TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Metrik henüz oluşmamışsa sessizce devam et
            }

            // TaskDispatcherService üzerinden havuz durum bilgilerini al
            int activeThreads = taskDispatcherService.getActiveThreadCount();
            int queuedTasks = taskDispatcherService.getQueueSize();
            int maxThreads = taskDispatcherService.getMaxThreads();

            // Verileri taşıyıcı nesneye (DTO) paketle
            SystemMetrics metrics = SystemMetrics.builder()
                    .heapUsedMb(heapUsed)
                    .heapMaxMb(heapMax)
                    .gcPauseCount(gcCount)
                    .gcPauseTotalTimeMs(gcTotalTime)
                    .activeThreads(activeThreads)
                    .queuedTasks(queuedTasks)
                    .maxThreads(maxThreads)
                    .build();

            // WebSocket üzerinden frontend'e (React) fırlat
            messagingTemplate.convertAndSend("/topic/metrics", metrics);

        } catch (Exception e) {
            // Kritik olmayan hataları standart hata çıktısına yazdır
            System.err.println("Metrik okunurken hata: " + e.getMessage());
        }
    }
}
