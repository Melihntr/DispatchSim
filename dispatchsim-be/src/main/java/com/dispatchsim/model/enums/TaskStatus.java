package com.dispatchsim.model.enums;

public enum TaskStatus {
    WAITING,
    RUNNING,
    BLOCKED,   // YENİ EKLENDİ: Deadlock durumunda buraya geçecek
    SUCCESS,
    TIMEOUT,
    FAILED,
    CANCELLED
}