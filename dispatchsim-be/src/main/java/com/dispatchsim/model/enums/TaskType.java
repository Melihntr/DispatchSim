package com.dispatchsim.model.enums;

public enum TaskType {
    CPU_BOUND, // İşlemciyi yoran, thread'i meşgul eden görevler
    IO_BOUND   // Bekleme süresi olan (örn. sahte veritabanı/network çağrısı) görevler
}