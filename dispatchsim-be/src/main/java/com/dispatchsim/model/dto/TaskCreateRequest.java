package com.dispatchsim.model.dto;

import com.dispatchsim.model.enums.Priority;
import com.dispatchsim.model.enums.TaskType;
import lombok.Data;

/**
 * Kullanıcı tarafından yeni bir görev (Task) oluşturulurken gönderilen veri transfer nesnesidir.
 * <p>
 * Bu sınıf, simülasyonun türünü ve işleme önceliğini belirleyen temel parametreleri içerir.
 * Frontend üzerinden gelen POST isteklerinin gövdesini (request body) temsil eder.
 * </p>
 *
 * @author Melihntr
 */
@Data
public class TaskCreateRequest {

    /**
     * Görevin işlem karakteristiğini belirler.
     * <ul>
     * <li><b>CPU_BOUND:</b> Yoğun işlemci gücü gerektiren, genellikle 3 saniye süren görevler.</li>
     * <li><b>IO_BOUND:</b> Giriş/Çıkış bekleyen (dosya, DB, network), genellikle 1.5 saniye süren görevler.</li>
     * </ul>
     */
    private TaskType type;

    /**
     * Görevin kuyruktaki işlenme sırasını belirleyen öncelik seviyesi.
     * <p>
     * {@link com.dispatchsim.service.DispatchTask} içindeki karşılaştırma mantığına göre,
     * önceliği yüksek olan görevler (örn: CRITICAL), kuyrukta kendinden daha önce gelen
     * düşük öncelikli görevlerin önüne geçer.
     * </p>
     * Seviyeler: {@code LOW}, {@code MEDIUM}, {@code HIGH}, {@code CRITICAL}
     */
    private Priority priority;
}