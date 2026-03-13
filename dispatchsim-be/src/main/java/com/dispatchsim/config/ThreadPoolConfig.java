package com.dispatchsim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Uygulama genelinde kullanılacak farklı iş parçacığı havuzu (Thread Pool)
 * stratejilerini yapılandıran konfigürasyon sınıfıdır.
 * * Bu sınıf, geleneksel havuzlar ile Project Loom (Sanal Thread'ler) arasındaki
 * performans farklarını ve davranış değişimlerini gözlemlemek için tasarlanmıştır.
 *
 * @author Melihntr
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * Geleneksel, öncelik tabanlı sabit bir iş parçacığı havuzu oluşturur.
     * * Bu havuz, kaynak kısıtlılığı (resource contention) ve düşük öncelikli görevlerin
     * kaynak beklediği 'Starvation' (Açlık) senaryolarını simüle etmek için kullanılır.
     * * Özellikler:
     * - Çekirdek (Core) Boyutu: 4
     * - Maksimum Boyut: 8
     * - Kuyruk Yapısı: PriorityBlockingQueue (Öncelik sıralı)
     * * @return Yapılandırılmış {@link ThreadPoolExecutor} nesnesi
     */
    @Bean("fixedThreadPool")
    public ThreadPoolExecutor fixedThreadPool() {
        return new ThreadPoolExecutor(
                4, 8, 60L, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(500)
        );
    }

    /**
     * Project Loom kapsamında sunulan Sanal İş Parçacığı (Virtual Thread) havuzunu oluşturur.
     * * Binlerce IO-Bound (Giriş/Çıkış odaklı) görevin, işletim sistemi thread'lerini
     * tüketmeden ve sistemi çökertmeden nasıl eşzamanlı çalışabildiğini test etmek
     * amacıyla kullanılır.
     * * @return Her görev için yeni bir sanal thread oluşturan {@link ExecutorService}
     */
    @Bean("virtualThreadPool")
    public ExecutorService virtualThreadPool() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * İhtiyaca göre dinamik olarak büyüyen ve boşta kalan thread'leri geri kazanan
     * önbellekli iş parçacığı havuzu oluşturur.
     * * Çok çekirdekli işlemcilerde dinamik ölçeklendirme ve hızlı işleme analizi
     * yapmak için yapılandırılmıştır.
     * * @return Dinamik ölçeklenen {@link ExecutorService}
     */
    @Bean("cachedThreadPool")
    public ExecutorService cachedThreadPool() {
        return Executors.newCachedThreadPool();
    }
}