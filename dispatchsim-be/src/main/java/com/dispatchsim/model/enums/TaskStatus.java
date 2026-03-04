package com.dispatchsim.model.enums;

public enum TaskStatus {
    WAITING,   // Kuyrukta bekliyor
    RUNNING,   // Thread tarafından işleniyor
    SUCCESS,   // Başarıyla bitti
    TIMEOUT,   // Süre aşımına uğradı
    FAILED,    // Hata aldı
    CANCELLED  // İptal edildi
}