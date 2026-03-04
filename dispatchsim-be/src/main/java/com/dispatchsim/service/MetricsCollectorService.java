package com.dispatchsim.service;

import com.dispatchsim.model.dto.SystemMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MetricsCollectorService {

    private final MeterRegistry meterRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final ThreadPoolExecutor dispatchExecutor; // Kuyruk ve thread durumunu okumak için

    // Her 1 saniyede bir (1000 ms) çalışıp frontend'e sistem durumunu fırlatır
    @Scheduled(fixedRate = 1000)
    public void publishMetrics() {
        try {
            // JVM Heap Memory kullanımı (Byte -> Megabyte çevrimi)
            double heapUsed = meterRegistry.get("jvm.memory.used").tag("area", "heap").gauge().value() / (1024.0 * 1024.0);
            double heapMax = meterRegistry.get("jvm.memory.max").tag("area", "heap").gauge().value() / (1024.0 * 1024.0);

            // GC Duraklama (Pause) metrikleri
            long gcCount = 0;
            double gcTotalTime = 0;
            try {
                gcCount = (long) meterRegistry.get("jvm.gc.pause").timer().count();
                gcTotalTime = meterRegistry.get("jvm.gc.pause").timer().totalTime(TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Uygulama ilk kalktığında GC event'i henüz oluşmamış olabilir, sessizce geç.
            }

            // Thread ve Kuyruk durumu
            int activeThreads = dispatchExecutor.getActiveCount();
            int queuedTasks = dispatchExecutor.getQueue().size();

            // DTO'yu oluştur
            SystemMetrics metrics = SystemMetrics.builder()
                    .heapUsedMb(heapUsed)
                    .heapMaxMb(heapMax)
                    .gcPauseCount(gcCount)
                    .gcPauseTotalTimeMs(gcTotalTime)
                    .activeThreads(activeThreads)
                    .queuedTasks(queuedTasks)
                    .build();

            // WebSocket üzerinden yayınla
            messagingTemplate.convertAndSend("/topic/metrics", metrics);

        } catch (Exception e) {
            // Metrik okuma sırasında geçici hatalar olabilir, logla ama sistemi durdurma
            System.err.println("Metrik okunurken hata: " + e.getMessage());
        }
    }
}
