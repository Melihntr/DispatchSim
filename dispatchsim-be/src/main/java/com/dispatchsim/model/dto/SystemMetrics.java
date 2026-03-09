package com.dispatchsim.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetrics {
    
    // Memory (Bellek) Verileri
    private double heapUsedMb;
    private double heapMaxMb;
    
    // Garbage Collector (Çöp Toplayıcı) Verileri
    private long gcPauseCount;
    private double gcPauseTotalTimeMs;
    
    // Thread ve Kuyruk Verileri
    private int activeThreads;
    private int queuedTasks;
    private int maxThreads; // YENİ EKLENEN ALAN
    
}
