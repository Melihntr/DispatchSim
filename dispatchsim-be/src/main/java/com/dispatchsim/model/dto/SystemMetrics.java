package com.dispatchsim.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Sistemin anlık sağlık ve performans verilerini taşıyan veri transfer nesnesidir (DTO).
 * <p>
 * Bu nesne, {@code MetricsCollectorService} tarafından her saniye oluşturulur ve
 * WebSocket üzerinden frontend paneline (Dashboard) gönderilir. Bellek kullanımı,
 * GC (Garbage Collection) istatistikleri ve iş parçacığı havuzu durumunu tek bir paket halinde sunar.
 * </p>
 *
 * @author MELİHNTR
 * @see com.dispatchsim.service.MetricsCollectorService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetrics {

    /** JVM tarafından o anda kullanılan aktif Heap bellek miktarı (Megabayt cinsinden). */
    private double heapUsedMb;

    /** JVM'in çıkabileceği maksimum Heap bellek sınırı (Megabayt cinsinden). */
    private double heapMaxMb;

    /** Uygulamanın başlangıcından itibaren gerçekleşen toplam Garbage Collection duraklama sayısı. */
    private long gcPauseCount;

    /** GC işlemlerinin sistemi toplamda kaç milisaniye durdurduğunu (Stop-the-world) gösteren süre. */
    private double gcPauseTotalTimeMs;

    /** İş parçacığı havuzunda (ThreadPool) o anda aktif olarak bir görev yürüten thread sayısı. */
    private int activeThreads;

    /** Havuzdaki tüm threadler dolu olduğu için kuyrukta (Queue) beklemeye alınan görev sayısı. */
    private int queuedTasks;

    /** * İş parçacığı havuzunun dinamik olarak çıkabileceği en üst thread sınırı.
     * Auto-scaling simülasyonu sırasında bu değerin değişimi izlenebilir.
     */
    private int maxThreads;
}
