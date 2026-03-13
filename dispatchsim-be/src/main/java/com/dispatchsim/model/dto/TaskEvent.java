package com.dispatchsim.model.dto;

import com.dispatchsim.model.entity.TaskEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Görevlerin durum değişikliklerini ve sistem olaylarını temsil eden olay (event) paketidir.
 * <p>
 * {@link com.dispatchsim.service.WebSocketPublisher} tarafından kullanılan bu sınıf,
 * asenkron yürütülen görevlerin o anki durumunu (durum kodu ve güncel veri)
 * frontend uygulamasına (React) anlık olarak bildirmek için tasarlanmıştır.
 * </p>
 *
 * @author Melihntr
 */
@Data
@AllArgsConstructor
public class TaskEvent {

    /**
     * Olayın türünü belirten etiket.
     * <p>
     * Yaygın değerler:
     * <ul>
     * <li>{@code WAITING}: Görev kuyruğa alındı.</li>
     * <li>{@code RUNNING}: Görev bir thread tarafından işlenmeye başlandı.</li>
     * <li>{@code SUCCESS}: Görev başarıyla tamamlandı.</li>
     * <li>{@code FAILED}: Görev bir hata nedeniyle durdu.</li>
     * <li>{@code BLOCKED}: Deadlock senaryosunda görev kilitlendi.</li>
     * <li>{@code CLEAR_ALL}: Tüm sistemin sıfırlandığını bildiren özel olay.</li>
     * </ul>
     * </p>
     */
    private String eventType;

    /**
     * Olayla ilişkili olan güncel görev verisi.
     * {@code CLEAR_ALL} gibi sistem genelindeki olaylarda bu alan {@code null} olabilir.
     */
    private TaskEntity task;
}
