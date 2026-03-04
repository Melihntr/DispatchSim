import React, { useEffect, useState } from 'react';
import ReactFlow, { Background, Controls, MiniMap } from 'reactflow';
import 'reactflow/dist/style.css'; // React Flow'un zorunlu CSS dosyası

import { useWebSocket } from './hooks/useWebsocket.js'; // İsimdeki büyük/küçük harf düzeltildi
import { createTask } from './services/api';

// Task durumlarına göre Node (Kutu) renklerini belirleyelim
const getColorByStatus = (status) => {
    switch (status) {
        case 'WAITING': return '#e2e8f0'; // Gri
        case 'RUNNING': return '#86efac'; // Yeşil
        case 'SUCCESS': return '#93c5fd'; // Mavi
        case 'FAILED': return '#fca5a5';  // Kırmızı
        case 'CANCELLED': return '#fcd34d'; // Sarı
        default: return '#ffffff';
    }
};

export default function App() {
    // Hook'umuzdan canlı verileri çekiyoruz
    const { tasks, metrics } = useWebSocket();
    const [nodes, setNodes] = useState([]);

    // Yeni task tetikleme fonksiyonu
    const handleCreateTask = async (type, priority) => {
        try {
            await createTask(type, priority);
        } catch (error) {
            console.error("Task oluşturulamadı:", error);
        }
    };

    // WebSocket'ten yeni 'tasks' geldiğinde React Flow Node'larını güncelle
    useEffect(() => {
        // Durumlarına göre görevleri ayırıyoruz ki ekranda sütunlara dizelim
        const waitingTasks = tasks.filter(t => t.status === 'WAITING');
        const runningTasks = tasks.filter(t => t.status === 'RUNNING');
        const completedTasks = tasks.filter(t => ['SUCCESS', 'FAILED', 'TIMEOUT', 'CANCELLED'].includes(t.status));

        const createNode = (task, index, xOffset) => ({
            id: task.id.toString(),
            position: { 
                x: xOffset, 
                y: index * 120 + 50 // Yukarıdan aşağıya doğru diz
            },
            data: { 
                label: (
                    <div style={{ textAlign: 'center', padding: '5px' }}>
                        <strong>ID: {task.id}</strong><br/>
                        <span style={{ fontSize: '12px' }}>{task.type}</span><br/>
                        <span style={{ fontSize: '12px', fontWeight: 'bold' }}>Priority: {task.priority}</span><br/>
                        <span style={{ fontSize: '10px' }}>{task.status}</span>
                    </div>
                )
            },
            style: { 
                background: getColorByStatus(task.status),
                border: '1px solid #333',
                borderRadius: '8px',
                width: 150,
                color: '#000',
                boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
            }
        });

        const newNodes = [
            // Bekleyenleri x: 50 hizasına diz
            ...waitingTasks.map((t, i) => createNode(t, i, 50)),
            // Çalışanları x: 350 hizasına diz (Burası Thread Pool'umuz)
            ...runningTasks.map((t, i) => createNode(t, i, 350)),
            // Bitenleri x: 650 hizasına diz (En fazla son 10 biteni gösterelim ekran dolmasın)
            ...completedTasks.slice(-10).map((t, i) => createNode(t, i, 650))
        ];

        setNodes(newNodes);
    }, [tasks]);

    return (
        // DÜZELTME BURADA: width: '100%' yapıldı ve overflow: 'hidden' eklendi
        <div style={{ height: '100vh', width: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden', margin: 0, padding: 0 }}>
            
            {/* Üst Panel: Kontroller ve Metrikler */}
            <div style={{ padding: '20px', background: '#1e293b', color: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                    <h2>DispatchSim Control Panel</h2>
                    <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                        <button onClick={() => handleCreateTask('CPU_BOUND', 'LOW')} style={{ padding: '8px', cursor: 'pointer' }}>+ CPU (Low)</button>
                        <button onClick={() => handleCreateTask('CPU_BOUND', 'CRITICAL')} style={{ background: '#ef4444', color: 'white', padding: '8px', cursor: 'pointer' }}>+ CPU (Critical)</button>
                        <button onClick={() => handleCreateTask('IO_BOUND', 'MEDIUM')} style={{ padding: '8px', cursor: 'pointer' }}>+ IO (Medium)</button>
                    </div>
                </div>

                {/* Sağ Üst: Canlı JVM Metrikleri */}
                {metrics && (
                    <div style={{ background: '#334155', padding: '15px', borderRadius: '8px', minWidth: '300px' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '13px' }}>
                            <span><strong>Threads:</strong> {metrics.activeThreads} / 8</span>
                            <span><strong>Queue:</strong> {metrics.queuedTasks}</span>
                            <span><strong>GC Pauses:</strong> {metrics.gcPauseCount}</span>
                        </div>
                        
                        {/* Memory Progress Bar */}
                        <div style={{ fontSize: '12px', marginBottom: '4px', textAlign: 'right' }}>
                            Heap: {metrics.heapUsedMb.toFixed(0)} MB / {metrics.heapMaxMb.toFixed(0)} MB
                        </div>
                        <div style={{ width: '100%', background: '#1e293b', height: '12px', borderRadius: '6px', overflow: 'hidden' }}>
                            <div style={{ 
                                height: '100%', 
                                width: `${Math.min((metrics.heapUsedMb / metrics.heapMaxMb) * 100, 100)}%`,
                                background: (metrics.heapUsedMb / metrics.heapMaxMb) > 0.8 ? '#ef4444' : (metrics.heapUsedMb / metrics.heapMaxMb) > 0.5 ? '#f59e0b' : '#10b981',
                                transition: 'width 0.5s ease-in-out, background-color 0.5s ease'
                            }}></div>
                        </div>
                    </div>
                )}
            </div>

            {/* Alt Panel: React Flow Canvas (Tuval) */}
            <div style={{ flex: 1, position: 'relative', background: '#f8fafc' }}>
                <ReactFlow nodes={nodes} edges={[]}>
                    <Background />
                    <Controls />
                    <MiniMap />
                </ReactFlow>
            </div>
        </div>
    );
}