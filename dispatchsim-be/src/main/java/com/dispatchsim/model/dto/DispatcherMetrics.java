package com.dispatchsim.model.dto;

/**
 * Sistem dispatcher metriklerini taşıyan DTO.
 *
 * <p>
 * Bu metrikler sistem dashboard'unda thread kullanımı,
 * queue yoğunluğu ve loom thread durumunu göstermek için kullanılır.
 * </p>
 */
public record DispatcherMetrics(

        /**
         * Aktif çalışan thread sayısı.
         */
        int activeThreads,

        /**
         * Havuzda mevcut bulunan toplam thread sayısı.
         */
        int currentPoolSize,

        /**
         * Havuzun ulaşabileceği maksimum thread kapasitesi.
         */
        int maxThreads,

        /**
         * Kuyrukta bekleyen görev sayısı.
         */
        int queueSize,

        /**
         * Loom (virtual thread) simülasyonu sırasında aktif thread sayısı.
         */
        int loomActiveThreads
) {}