package com.dispatchsim.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Uygulama genelindeki zamanlanmış görevleri (Scheduled Tasks) aktifleştiren ve 
 * yöneten konfigürasyon sınıfıdır.
 * <p>
 * Örneğin; MetricsCollectorService içindeki saniyelik metrik toplama 
 * işlemleri bu altyapıyı kullanır. Ayrı bir sınıfta olması, test ortamlarında 
 * zamanlanmış görevlerin kolayca devre dışı bırakılabilmesini sağlar.
 * </p>
 *
 * @author Melihntr
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // İleride zamanlanmış görevler için özel bir thread havuzu (ThreadPoolTaskScheduler) 
    // ayarlamak istersek tam olarak buraya ekleyeceğiz.
}