package com.dispatchsim.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Uygulamanın Cross-Origin Resource Sharing (CORS) politikalarını yöneten global yapılandırma sınıfıdır.
 * <p>
 * Frontend uygulamalarının (React, Vue vb.) farklı bir port veya domain üzerinden 
 * REST API'ye güvenli bir şekilde istek atabilmesini sağlar. Controller seviyesindeki 
 * @CrossOrigin anotasyonlarına olan ihtiyacı ortadan kaldırır.
 * </p>
 *
 * @author Melihntr
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CORS kurallarını kayıt defterine ekler.
     * @param registry CORS kayıt defteri
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // Sadece /api/ ile başlayan endpointler için geçerli olsun
                .allowedOriginPatterns("*") // Geliştirme ortamı için her yere açık (Prodda spesifik URL yazılır)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // İzin verilen HTTP metodları
                .allowedHeaders("*") // İzin verilen headerlar
                .allowCredentials(false); // Cookie veya Auth bilgisi taşınacaksa true yapılır
    }
}