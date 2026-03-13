package com.dispatchsim.service;

import com.dispatchsim.model.dto.TaskEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Görev durumlarındaki değişiklikleri WebSocket üzerinden istemcilere duyuran servis sınıfıdır.
 * <p>
 * Bu servis, asenkron olarak çalışan görevlerin yaşam döngüsü olaylarını
 * (oluşturulma, çalışma, tamamlanma veya hata durumları) yakalar ve
 * bunları STOMP protokolü üzerinden yayınlar.
 * </p>
 *
 * @author Melihntr
 */
@Service
@RequiredArgsConstructor
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Bir görevin durum değişim olayını {@code /topic/tasks} kanalına yayınlar.
     * <p>
     * Frontend (React) tarafı bu kanalı dinleyerek listedeki görevlerin
     * renklerini ve durumlarını anlık olarak günceller.
     * </p>
     * * @param event Yayınlanacak olan görev olay nesnesi (ID, Durum, Veri vb.)
     */
    public void publishTaskUpdate(TaskEvent event) {
        // STOMP mesaj şablonu üzerinden veriyi JSON olarak belirtilen hedefe fırlatır
        messagingTemplate.convertAndSend("/topic/tasks", event);
    }
}