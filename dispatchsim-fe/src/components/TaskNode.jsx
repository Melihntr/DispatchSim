import React from 'react';
import { Handle, Position } from 'reactflow';
import './TaskNode.css'; // Temizlenmiş CSS'i içeri alıyoruz

// Renk paletini bileşenin dışına aldık ki her re-render'da baştan oluşturulmasın (Performans Optimizasyonu)
const STATUS_COLORS = {
    WAITING: '#94a3b8',   // Gri
    RUNNING: '#3b82f6',   // Mavi
    BLOCKED: '#9333ea',   // Koyu Mor
    SUCCESS: '#10b981',   // Yeşil
    FAILED: '#ef4444',    // Kırmızı
    CANCELLED: '#f59e0b', // Turuncu
    TIMEOUT: '#ec4899',   // Pembe
};

export default function TaskNode({ data }) {
    // 🛡️ SAVUNMACI PROGRAMLAMA: Ya data boş gelirse? Ya task silinmişse? 
    // Hata fırlatmak yerine null (hiçbir şey) çizip uygulamayı kurtarıyoruz.
    const task = data?.task;
    if (!task) return null;

    // Güvenli değişken atamaları
    const status = task.status || 'WAITING';
    const color = STATUS_COLORS[status] || '#ffffff';
    const isRunning = status === 'RUNNING';
    const isBlocked = status === 'BLOCKED';

    // Dinamik Stiller (Sadece duruma göre değişenleri burada tutuyoruz)
    const dynamicContainerStyle = {
        borderTop: `6px solid ${color}`,
        boxShadow: isRunning ? '0 0 20px rgba(59, 130, 246, 0.6)' : '0 4px 6px -1px rgba(0, 0, 0, 0.1)'
    };

    const dynamicPriorityColor = 
        task.priority === 'CRITICAL' ? '#ef4444' : 
        task.priority === 'HIGH' ? '#f59e0b' : '#334155';

    return (
        <div 
            className={`task-node-container ${isBlocked ? 'shake-animation' : ''}`} 
            style={dynamicContainerStyle}
        >
            {/* Üst Kısım: ID ve Status Badge */}
            <div className="task-node-header">
                <span className="task-id-text">
                    #{task.id || 'N/A'}
                </span>
                <span 
                    className={`task-status-badge ${isRunning ? 'pulse-animation' : ''}`} 
                    style={{ background: color }}
                >
                    {status}
                </span>
            </div>

            {/* Orta Kısım: Detaylar */}
            <div className="task-details-wrapper">
                <div className="task-detail-row">
                    <span className="detail-label">Tipi:</span>
                    <span className="detail-value-type">
                        {task.type === 'CPU_BOUND' ? '⚙️ CPU' : '🌐 IO'}
                    </span>
                </div>
                
                <div className="task-detail-row">
                    <span className="detail-label">Öncelik:</span>
                    <span 
                        className="detail-value-priority"
                        style={{ color: dynamicPriorityColor }}
                    >
                        {task.priority === 'CRITICAL' ? '🚨 CRITICAL' : (task.priority || 'NORMAL')}
                    </span>
                </div>
            </div>

            {/* React Flow Bağlantı Noktaları (Gizli) */}
            <Handle type="target" position={Position.Left} style={{ opacity: 0 }} />
            <Handle type="source" position={Position.Right} style={{ opacity: 0 }} />
        </div>
    );
}