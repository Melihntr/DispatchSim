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

@Service
@RequiredArgsConstructor
public class MetricsCollectorService {

    private final MeterRegistry meterRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    // DÜZELTME BURADA: Artık doğrudan ThreadPool'u değil, bizim servisi alıyoruz
    private final TaskDispatcherService taskDispatcherService;

    // Her 1 saniyede bir (1000 ms) çalışıp frontend'e sistem durumunu fırlatır


    // ... Sınıfın içi ...

    @Scheduled(fixedRate = 1000)
    public void publishMetrics() {
        try {
            // --- DÜZELTİLEN KISIM: Doğrudan JVM Management Factory'den okuyoruz ---
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            double heapUsed = memoryBean.getHeapMemoryUsage().getUsed() / (1024.0 * 1024.0);
            double heapMax = memoryBean.getHeapMemoryUsage().getMax() / (1024.0 * 1024.0);
            // ----------------------------------------------------------------------

            // GC Duraklama (Pause) metrikleri (Burası Micrometer ile kalabilir, sorunsuz çalışır)
            long gcCount = 0;
            double gcTotalTime = 0;
            try {
                gcCount = (long) meterRegistry.get("jvm.gc.pause").timer().count();
                gcTotalTime = meterRegistry.get("jvm.gc.pause").timer().totalTime(TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // Sessizce geç
            }

            int activeThreads = taskDispatcherService.getActiveThreadCount();
            int queuedTasks = taskDispatcherService.getQueueSize();
            int maxThreads = taskDispatcherService.getMaxThreads();

            // DTO'yu oluştur
            SystemMetrics metrics = SystemMetrics.builder()
                    .heapUsedMb(heapUsed)
                    .heapMaxMb(heapMax)
                    .gcPauseCount(gcCount)
                    .gcPauseTotalTimeMs(gcTotalTime)
                    .activeThreads(activeThreads)
                    .queuedTasks(queuedTasks)
                    .maxThreads(maxThreads)
                    .build();

            // WebSocket üzerinden yayınla
            messagingTemplate.convertAndSend("/topic/metrics", metrics);

        } catch (Exception e) {
            System.err.println("Metrik okunurken hata: " + e.getMessage());
        }
    }
}
