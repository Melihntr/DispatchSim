package com.dispatchsim.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Uygulamanın ana görev yürütme (dispatch) thread havuzunu tanımlar.
 *
 * <p>
 * Thread havuzu merkezi olarak Spring konteyneri tarafından yönetilir.
 * Böylece servis içerisinde yeni executor yaratılması veya runtime'da
 * değiştirilmesi gibi concurrency problemleri önlenmiş olur.
 * </p>
 *
 * <p>
 * Havuz özellikleri:
 * <ul>
 *     <li>Core Thread: 4</li>
 *     <li>Max Thread: 8</li>
 *     <li>Queue: PriorityBlockingQueue</li>
 * </ul>
 * </p>
 */
@Configuration
public class ExecutorConfig {

    @Bean
    public ThreadPoolExecutor dispatchExecutor() {
        return new ThreadPoolExecutor(
                4,
                8,
                60L,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(100)
        );
    }
}
