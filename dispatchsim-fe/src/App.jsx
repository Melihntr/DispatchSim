import React, { useEffect, useState, useMemo } from 'react';
import ReactFlow, { Background, Controls, MiniMap } from 'reactflow';
import 'reactflow/dist/style.css';

import { useWebSocket } from './hooks/useWebSocket.js';
import { createTask, clearAllTasks, triggerDeadlock, triggerStarvation, triggerLoom, triggerCircuitBreaker, triggerTimeout } from './services/api';
import TaskNode from './components/TaskNode'; // YENİ: Custom Node'umuzu import ettik

export default function App() {
    const { tasks, metrics } = useWebSocket();
    const [nodes, setNodes] = useState([]);

    // YENİ: React Flow'a "custom" adında yeni bir node tipi öğretiyoruz
    const nodeTypes = useMemo(() => ({ custom: TaskNode }), []);

    const handleCreateTask = async (type, priority) => {
        try {
            await createTask(type, priority);
        } catch (error) {
            console.error("Task oluşturulamadı:", error);
        }
    };

    useEffect(() => {
        const waitingTasks = tasks.filter(t => t.status === 'WAITING');
        const runningTasks = tasks.filter(t => t.status === 'RUNNING' || t.status === 'BLOCKED');
        const completedTasks = tasks.filter(t => ['SUCCESS', 'FAILED', 'TIMEOUT', 'CANCELLED'].includes(t.status));

        // Eski karmaşık style ve label tanımlarını sildik, sadece datayı TaskNode'a yolluyoruz
        const createNode = (task, index, xOffset) => ({
            id: task.id.toString(),
            type: 'custom', // YENİ: Bizim özel bileşeni kullan
            position: { x: xOffset, y: index * 140 + 50 }, // Aralarını biraz açtık
            data: { task: task } // Tüm task verisini Custom Node'a paslıyoruz
        });

        const newNodes = [
            ...waitingTasks.map((t, i) => createNode(t, i, 50)),
            ...runningTasks.map((t, i) => createNode(t, i, 350)),
            ...completedTasks.slice(-10).map((t, i) => createNode(t, i, 650))
        ];

        setNodes(newNodes);
    }, [tasks]);

    return (
        <div style={{ height: '100vh', width: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

            {/* Üst Panel */}
            <div style={{ padding: '20px 30px', background: '#0f172a', color: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center', boxShadow: '0 4px 6px rgba(0,0,0,0.3)', zIndex: 10 }}>
                <div>
                    <h2 style={{ margin: '0 0 15px 0', fontSize: '22px', fontWeight: '800', letterSpacing: '1px' }}>
                        ⚡ DispatchSim Console
                    </h2>
                    <div style={{ display: 'flex', gap: '12px' }}>
                        <button className="modern-btn btn-blue" onClick={() => handleCreateTask('CPU_BOUND', 'LOW')}>
                            ⚙️ CPU (Low)
                        </button>
                        <button className="modern-btn btn-red" onClick={() => handleCreateTask('CPU_BOUND', 'CRITICAL')}>
                            🚨 CPU (Critical)
                        </button>
                        <button className="modern-btn btn-purple" onClick={() => handleCreateTask('IO_BOUND', 'MEDIUM')}>
                            🌐 IO (Medium)
                        </button>
                        <button className="modern-btn" style={{ background: '#000', border: '1px solid #334155' }} onClick={async () => await clearAllTasks()}>
                            🗑️ Temizle
                        </button>
                        <button className="modern-btn" style={{ background: '#7e22ce' }} onClick={async () => await triggerDeadlock()}>
                            ☠️ Deadlock Başlat
                        </button>
                        <button className="modern-btn" style={{ background: '#ea580c', marginLeft: '10px' }} onClick={async () => await triggerStarvation()}>
                            ⏳ Starvation (Açlık)
                        </button>
                        <button className="modern-btn" style={{ background: '#4b5563' }} onClick={async () => await triggerLoom(false)}>
                            🐢 Platform (50 Task)
                        </button>
                        <button className="modern-btn" style={{ background: '#059669', boxShadow: '0 0 10px rgba(5,150,105,0.5)' }} onClick={async () => await triggerLoom(true)}>
                            🚀 Virtual (50 Task)
                        </button>
                        <button className="modern-btn" style={{ background: '#be123c', boxShadow: '0 0 10px rgba(190,18,60,0.5)' }} onClick={async () => await triggerCircuitBreaker()}>
                            🔌 Şalter (Circuit)
                        </button>
                        <button className="modern-btn" style={{ background: '#ec4899' }} onClick={async () => await triggerTimeout()}>
                            ⏱️ Timeout Testi
                        </button>
                    </div>
                </div>

                {/* Metrikler Paneli */}
                {metrics && (
                    <div style={{ background: '#1e293b', padding: '16px', borderRadius: '12px', minWidth: '320px', border: '1px solid #334155' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '12px', fontSize: '13px', color: '#94a3b8' }}>
                            <span><strong>Threads:</strong> <span style={{ color: 'white' }}>{metrics.activeThreads} / 8</span></span>
                            <span><strong>Queue:</strong> <span style={{ color: 'white' }}>{metrics.queuedTasks}</span></span>
                            <span><strong>GC:</strong> <span style={{ color: 'white' }}>{metrics.gcPauseCount}</span></span>
                        </div>

                        <div style={{ fontSize: '12px', marginBottom: '6px', textAlign: 'right', fontWeight: 'bold', color: '#cbd5e1' }}>
                            Heap: {metrics.heapUsedMb.toFixed(0)} MB / {metrics.heapMaxMb.toFixed(0)} MB
                        </div>
                        <div style={{ width: '100%', background: '#0f172a', height: '14px', borderRadius: '8px', overflow: 'hidden', border: '1px solid #334155' }}>
                            <div style={{
                                height: '100%',
                                width: `${Math.min((metrics.heapUsedMb / metrics.heapMaxMb) * 100, 100)}%`,
                                background: (metrics.heapUsedMb / metrics.heapMaxMb) > 0.8 ? '#ef4444' : (metrics.heapUsedMb / metrics.heapMaxMb) > 0.5 ? '#f59e0b' : '#10b981',
                                transition: 'width 0.5s cubic-bezier(0.4, 0, 0.2, 1), background-color 0.5s ease'
                            }}></div>
                        </div>
                    </div>
                )}
            </div>

            {/* React Flow Tuvali */}
            <div style={{ flex: 1, position: 'relative', background: '#f8fafc' }}>
                <ReactFlow nodes={nodes} edges={[]} nodeTypes={nodeTypes}>
                    <Background color="#cbd5e1" gap={20} size={2} />
                    <Controls />
                    <MiniMap
                        nodeColor={(node) => {
                            switch (node.data?.task?.status) {
                                case 'RUNNING': return '#3b82f6';
                                case 'SUCCESS': return '#10b981';
                                case 'FAILED': return '#ef4444';
                                default: return '#94a3b8';
                            }
                        }}
                    />
                </ReactFlow>
            </div>
        </div>
    );
}