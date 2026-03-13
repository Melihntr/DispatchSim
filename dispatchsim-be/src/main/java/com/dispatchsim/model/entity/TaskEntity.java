package com.dispatchsim.model.entity;

import com.dispatchsim.model.enums.Priority;
import com.dispatchsim.model.enums.TaskStatus;
import com.dispatchsim.model.enums.TaskType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Sistemdeki her bir görevin (task) veritabanındaki temsilidir.
 * <p>
 * Bu varlık (entity), görevin türü, önceliği ve yaşam döngüsü boyunca geçirdiği
 * zaman damgalarını (timestamps) saklar. H2 veritabanındaki "tasks" tablosuna
 * eşlenmiştir (mapping).
 * </p>
 *
 * @author Melihntr
 */
@Entity
@Table(name = "tasks")
@Data
public class TaskEntity {

    /** Görevin benzersiz kimlik numarası (Primary Key). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Görevin karakteristiği (CPU_BOUND veya IO_BOUND). */
    @Enumerated(EnumType.STRING)
    private TaskType type;

    /** Görevin kuyruktaki ağırlığını belirleyen öncelik seviyesi. */
    @Enumerated(EnumType.STRING)
    private Priority priority;

    /** Görevin anlık durumu (WAITING, RUNNING, SUCCESS, FAILED vb.). */
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    /** Görevin sisteme girdiği (veritabanına kaydedildiği) zaman. */
    private LocalDateTime createdAt;

    /** Görevin bir thread tarafından işlenmeye başlandığı zaman. */
    private LocalDateTime startedAt;

    /** Görevin işleminin bittiği (başarı veya hata ile) zaman. */
    private LocalDateTime finishedAt;

    /** Görevin aktif çalışma süresi (milisaniye cinsinden). */
    private Long executionTimeMs;

    /** * Görev tamamlandığında veya hata aldığında alınan tahmini bellek snapshot'ı.
     * Bellek sızıntısı analizleri için kullanılır.
     */
    private Long memoryAllocatedSnapshot;

    /**
     * Nesne veritabanına ilk kez kaydedilmeden önce otomatik olarak çalışır.
     * Oluşturulma tarihini ayarlar ve varsayılan durumu 'WAITING' olarak belirler.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = TaskStatus.WAITING;
        }
    }
}
