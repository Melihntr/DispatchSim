package com.dispatchsim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor dispatchExecutor() {
        // Çekirdek thread sayısı (Sürekli ayakta kalacak threadler)
        int corePoolSize = 4; 
        
        // Maksimum thread sayısı (Kuyruk dolduğunda çıkılabilecek tepe nokta)
        int maxPoolSize = 8;  
        
        // Boşta kalan ekstra threadlerin ne kadar süre sonra kapatılacağı
        long keepAliveTime = 60L; 

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(100) // Öncelik tabanlı, 100 kapasiteli başlangıç kuyruğu
        );
    }
}