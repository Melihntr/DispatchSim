import { create } from 'zustand';

// Zustand Depomuz: Uygulamanın beyni burası!
export const useSimulationStore = create((set) => ({
    // 1. STATE'LER (Tutacağımız Veriler)
    tasks: [],           // Görevlerin listesi
    metrics: {           // JVM Metrikleri
        activeThreads: 0,
        queueSize: 0,
        maxThreads: 8,
        memoryUsageMB: 0,
        memoryMaxMB: 0
    },
    isConnected: false,  // WebSocket bağlantı durumu

    // 2. AKSİYONLAR (Verileri Değiştiren Metotlar)

    // Yeni görev geldiğinde veya mevcut görev güncellendiğinde çalışır
    updateTask: (taskEvent) => set((state) => {
        const { eventType, task } = taskEvent;
        
        // CLEAR_ALL gelirse listeyi boşalt
        if (eventType === 'CLEAR_ALL') {
            return { tasks: [] };
        }

        // Mevcut görevi bul (Güncelleme mi yoksa yeni görev mi?)
        const existingTaskIndex = state.tasks.findIndex(t => t.id === task.id);

        if (existingTaskIndex >= 0) {
            // Görev zaten var, sadece o görevi güncelle (Performans için önemli!)
            const updatedTasks = [...state.tasks];
            updatedTasks[existingTaskIndex] = task;
            return { tasks: updatedTasks };
        } else {
            // Yeni görev gelmiş, listeye ekle
            return { tasks: [...state.tasks, task] };
        }
    }),

    // İlk sayfa yüklendiğinde geçmiş görevleri topluca set etmek için (REST API'den gelir)
    setInitialTasks: (tasks) => set({ tasks: tasks }),

    // Her saniye WebSocket'ten gelen metrikleri günceller
    updateMetrics: (newMetrics) => set({ metrics: newMetrics }),

    // Bağlantı durumunu UI'da (Yeşil nokta vs) göstermek için
    setConnectionStatus: (status) => set({ isConnected: status })
}));