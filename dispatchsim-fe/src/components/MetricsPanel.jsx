import React from 'react';
import './MetricsPanel.css'; // Stilleri içeri alıyoruz

export default function MetricsPanel({ metrics }) {
    if (!metrics) return null;

    // 🛡️ SAVUNMACI PROGRAMLAMA: Veriler undefined gelirse patlamasın diye fallback veriyoruz
    const heapUsed = metrics?.heapUsedMb || 0;
    const heapMax = metrics?.heapMaxMb || 1; // Sıfıra bölme hatası yememek için 1 veriyoruz
    const activeThreads = metrics?.activeThreads || 0;
    const maxThreads = metrics?.maxThreads || 8;
    const queuedTasks = metrics?.queuedTasks || 0;
    const gcCount = metrics?.gcPauseCount || 0;
    const gcTime = metrics?.gcPauseTotalTimeMs || 0;

    // Yüzdelik hesaplamalar
    const heapPercent = Math.min((heapUsed / heapMax) * 100, 100);
    const heapColor = heapPercent > 85 ? '#ef4444' : heapPercent > 60 ? '#f59e0b' : '#10b981';

    const threadPercent = Math.min((activeThreads / maxThreads) * 100, 100);
    const threadColor = threadPercent === 100 ? '#ef4444' : '#3b82f6';

    // Queue metninin rengi ve gölgesi (Dinamik olduğu için inline bırakıyoruz)
    const queueColor = queuedTasks > 10 ? '#ef4444' : queuedTasks > 0 ? '#f59e0b' : '#10b981';
    const queueShadow = queuedTasks > 10 ? '0 0 8px rgba(239, 68, 68, 0.6)' : 'none';

    return (
        <div className="metrics-panel-container">
            
            {/* SOL ÜST: THREADS */}
            <div className="metric-box">
                <div className="metric-header">
                    <span>Threads</span>
                    <span className="metric-value-text">{activeThreads}/{maxThreads}</span>
                </div>
                <div className="progress-bar-bg">
                    {/* Genişlik ve renk veriye göre değiştiği için inline kalmak zorunda */}
                    <div style={{ height: '100%', width: `${threadPercent}%`, background: threadColor, transition: 'width 0.3s ease' }}></div>
                </div>
            </div>

            {/* SAĞ ÜST: HEAP MEMORY */}
            <div className="metric-box">
                <div className="metric-header">
                    <span>Heap</span>
                    <span className="metric-value-text">{heapUsed.toFixed(0)}/{heapMax.toFixed(0)} MB</span>
                </div>
                <div className="progress-bar-bg">
                    {/* Genişlik ve renk dinamik */}
                    <div style={{ height: '100%', width: `${heapPercent}%`, background: heapColor, transition: 'width 0.5s ease' }}></div>
                </div>
            </div>

            {/* SOL ALT: QUEUE */}
            <div className="dark-info-box">
                <span className="info-label">Queue:</span>
                <span style={{ fontSize: '13px', fontWeight: '900', color: queueColor, textShadow: queueShadow }}>
                    {queuedTasks}
                </span>
            </div>

            {/* SAĞ ALT: GARBAGE COLLECTION */}
            <div className="dark-info-box gc-text" style={{ padding: '5px 8px' }}>
                <span>GC: <span className="gc-value">{gcCount}x</span></span>
                <span>Pause: <span className="gc-value">{gcTime.toFixed(0)}ms</span></span>
            </div>
            
        </div>
    );
}