package com.dispatchsim.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsCollectorServiceTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TaskDispatcherService taskDispatcherService;

    @InjectMocks
    private MetricsCollectorService metricsCollectorService;

    @Test
    void testPublishMetrics_Success() {
        // İçteki try-catch'in catch kısmına düşüp kodun çökmediğini test etmek için Micrometer'ı patlatıyoruz
        when(meterRegistry.get(anyString())).thenThrow(new RuntimeException("Micrometer yok"));

        when(taskDispatcherService.getActiveThreadCount()).thenReturn(4);
        when(taskDispatcherService.getQueueSize()).thenReturn(10);
        when(taskDispatcherService.getMaxThreads()).thenReturn(8);

        metricsCollectorService.publishMetrics();

        // Her şeye rağmen WebSocket'e metrik gönderildi mi?
        verify(messagingTemplate).convertAndSend(eq("/topic/metrics"), any(Object.class));
    }

    @Test
    void testPublishMetrics_OuterExceptionHandling() {
        // En dıştaki catch bloğunun (System.err yazdıran kısmın) çalışıp çalışmadığını test ediyoruz
        when(taskDispatcherService.getActiveThreadCount()).thenThrow(new RuntimeException("Kritik Sistem Hatası"));

        // Metod çağrıldığında uygulama çökmemeli
        metricsCollectorService.publishMetrics();

        // Hata olduğu için WebSocket yayını YAPILMAMALI
        verifyNoInteractions(messagingTemplate);
    }
}
