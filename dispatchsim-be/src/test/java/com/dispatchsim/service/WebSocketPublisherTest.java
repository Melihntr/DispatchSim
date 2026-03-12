package com.dispatchsim.service;

import com.dispatchsim.model.dto.TaskEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketPublisher webSocketPublisher;

    @Test
    void testPublishTaskUpdate() {
        TaskEvent mockEvent = new TaskEvent("TEST", null); // Kendi DTO'na göre düzenle
        
        webSocketPublisher.publishTaskUpdate(mockEvent);

        // Mesajın doğru kanala fırlatıldığını doğrula
        verify(messagingTemplate).convertAndSend(eq("/topic/tasks"), eq(mockEvent));
    }
}
