package com.dispatchsim.service;

import com.dispatchsim.model.entity.TaskEntity;
import lombok.Getter;

/**
 * İş parçacığı havuzu tarafından yürütülecek olan ve öncelik tabanlı
 * sıralama yeteneğine sahip görev sarmalayıcı sınıfıdır.
 * <p>
 * Bu sınıf hem {@link Runnable} arayüzünü uygulayarak yürütülebilir bir iş birimi oluşturur,
 * hem de {@link Comparable} arayüzünü uygulayarak {@link java.util.concurrent.PriorityBlockingQueue}
 * içerisinde doğru sırada konumlanmayı sağlar.
 * </p>
 *
 * @author Melihntr
 */
@Getter
public class DispatchTask implements Runnable, Comparable<DispatchTask> {

    /** Görevin veritabanı ve durum bilgilerini içeren varlık nesnesi */
    private final TaskEntity taskEntity;

    /** Görev atandığında çalıştırılacak olan asıl iş mantığı (CPU/IO simülasyonu) */
    private final Runnable action;

    /**
     * Yeni bir yürütülebilir görev oluşturur.
     * * @param taskEntity Göreve ait meta veriler
     * @param action Görev tetiklendiğinde yürütülecek olan iş bloğu
     */
    public DispatchTask(TaskEntity taskEntity, Runnable action) {
        this.taskEntity = taskEntity;
        this.action = action;
    }

    /**
     * Görev bir thread tarafından devralındığında {@code action} içeriğini yürütür.
     */
    @Override
    public void run() {
        action.run();
    }

    /**
     * İki görev arasındaki öncelik sırasını belirler.
     * <p>
     * Sıralama iki aşamalı bir mantıkla çalışır:
     * <ol>
     * <li><b>Öncelik Kontrolü:</b> Daha yüksek önceliğe (CRITICAL > HIGH > MEDIUM > LOW)
     * sahip olan görevler kuyruğun başına geçer.</li>
     * <li><b>Zaman Kontrolü (FIFO):</b> Öncelikler eşitse, oluşturulma zamanı
     * ({@code createdAt}) daha eski olan görev öne geçer.</li>
     * </ol>
     * </p>
     * * @param other Karşılaştırılacak olan diğer görev nesnesi
     * @return Karşılaştırma sonucu (Negatif: Bu görev önde, Pozitif: Diğer görev önde)
     */
    @Override
    public int compareTo(DispatchTask other) {
        // 1. Kural: Önceliği yüksek olan (CRITICAL > HIGH > MEDIUM > LOW) öne geçer.
        int priorityCompare = other.taskEntity.getPriority().compareTo(this.taskEntity.getPriority());

        if (priorityCompare != 0) {
            return priorityCompare;
        }

        // 2. Kural: Eğer öncelikler aynıysa, sisteme ilk gelen (createdAt) öne geçer (FIFO).
        return this.taskEntity.getCreatedAt().compareTo(other.taskEntity.getCreatedAt());
    }
}
