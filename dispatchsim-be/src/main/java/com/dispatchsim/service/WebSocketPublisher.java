package com.dispatchsim.service;

import com.dispatchsim.model.dto.TaskEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    // Task durum değişimlerini frontend'e fırlatır
    public void publishTaskUpdate(TaskEvent event) {
        messagingTemplate.convertAndSend("/topic/tasks", event);
    }
    
    // İleride Memory ve GC metriklerini fırlatmak için buraya yeni metodlar ekleyeceğiz.
}