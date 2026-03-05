import React from 'react';
import { Handle, Position } from 'reactflow';
 
export default function TaskNode({ data }) {
    const { task } = data;
 
    // Modern SaaS renk paleti
    const statusColors = {
        WAITING: '#94a3b8',   // Gri (Kuyrukta)
        RUNNING: '#3b82f6',   // Mavi (İşleniyor)
        SUCCESS: '#10b981',   // Zümrüt Yeşili (Bitti)
        FAILED: '#ef4444',    // Kırmızı (Hata)
        CANCELLED: '#f59e0b', // Turuncu (İptal)
    };
 
    const color = statusColors[task.status] || '#ffffff';
    const isRunning = task.status === 'RUNNING';
 
    return (
        <div style={{
            background: 'white',
            borderRadius: '12px',
            padding: '16px',
            minWidth: '180px',
            // Çalışan task'e mavi bir parlama (glow) efekti, diğerlerine standart gölge veriyoruz
            boxShadow: isRunning ? '0 0 20px rgba(59, 130, 246, 0.6)' : '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
            borderTop: `6px solid ${color}`,
            fontFamily: '"Inter", system-ui, sans-serif',
            position: 'relative',
            transition: 'all 0.3s ease'
        }}>
            {/* Üst Kısım: ID ve Status Badge */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px', borderBottom: '1px solid #f1f5f9', paddingBottom: '8px' }}>
                <span style={{ fontWeight: '800', color: '#334155', fontSize: '14px' }}>
                    #{task.id}
                </span>
                <span className={isRunning ? 'pulse-animation' : ''} style={{
                    fontSize: '10px',
                    padding: '4px 8px',
                    borderRadius: '12px',
                    background: color,
                    color: 'white',
                    fontWeight: 'bold',
                    letterSpacing: '0.5px'
                }}>
                    {task.status}
                </span>
            </div>
 
            {/* Orta Kısım: Detaylar */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                    <span style={{ color: '#64748b', fontWeight: '500' }}>Tipi:</span>
                    <span style={{ fontWeight: '700', color: '#0f172a' }}>
                        {task.type === 'CPU_BOUND' ? '⚙️ CPU' : '🌐 IO'}
                    </span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                    <span style={{ color: '#64748b', fontWeight: '500' }}>Öncelik:</span>
                    <span style={{
                        fontWeight: '800',
                        color: task.priority === 'CRITICAL' ? '#ef4444' : task.priority === 'HIGH' ? '#f59e0b' : '#334155'
                    }}>
                        {task.priority === 'CRITICAL' ? '🚨 CRITICAL' : task.priority}
                    </span>
                </div>
            </div>
 
            {/* React Flow'un ileride oklarla bağlayabilmesi için bağlantı noktaları (Gizli) */}
            <Handle type="target" position={Position.Left} style={{ opacity: 0 }} />
            <Handle type="source" position={Position.Right} style={{ opacity: 0 }} />
        </div>
    );
}