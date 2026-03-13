package com.dispatchsim.repository;

import com.dispatchsim.model.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * {@link TaskEntity} nesneleri için veritabanı erişim işlemlerini yürüten depolama (repository) arayüzüdür.
 * <p>
 * Spring Data JPA kullanarak standart CRUD (Oluşturma, Okuma, Güncelleme, Silme) işlemlerini
 * ve veritabanı sorgularını soyutlar. Simülasyon sırasında oluşturulan tüm görevler
 * bu arayüz üzerinden H2 veritabanına kaydedilir.
 * </p>
 *
 * @see org.springframework.data.jpa.repository.JpaRepository
 * @author Melihntr
 */
@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    // Özel sorgu metodları eklendiğinde Javadoc buraya genişletilebilir.
}