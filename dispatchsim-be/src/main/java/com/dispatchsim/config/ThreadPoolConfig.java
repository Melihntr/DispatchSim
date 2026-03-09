package com.dispatchsim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Configuration
public class ThreadPoolConfig {

    // 1. Geleneksel Sabit Havuz (Contention ve Starvation görmek için)
    @Bean("fixedThreadPool")
    public ThreadPoolExecutor fixedThreadPool() {
        return new ThreadPoolExecutor(
                4, 8, 60L, TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(500)
        );
    }

    // 2. Virtual Threads (Project Loom - Limitsiz, hafif threadler)
    // IO_Bound işlerde binlerce thread'in çökmeden çalıştığını göstereceğiz.
    @Bean("virtualThreadPool")
    public ExecutorService virtualThreadPool() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    // 3. Cached Thread Pool (Geldikçe thread açan, Multi-core scaling analizi için)
    @Bean("cachedThreadPool")
    public ExecutorService cachedThreadPool() {
        return Executors.newCachedThreadPool();
    }
}