import React, { useEffect, useState, useMemo } from 'react';
import ReactFlow, { Background, Controls, MiniMap } from 'reactflow';
import 'reactflow/dist/style.css';

// 1. STİL DOSYAMIZI İÇERİ ALIYORUZ
import './App.css';

// 2. ZUSTAND DEPOMUZU İÇERİ ALIYORUZ
import { useSimulationStore } from './store/useSimulationStore';
import { useWebSocket } from './hooks/useWebSocket.js';

import {
    createTask, clearAllTasks, triggerDeadlock, triggerStarvation,
    triggerCircuitBreaker, triggerTimeout, triggerLoom,
    triggerAutoScale, triggerMemoryLeak
} from './services/api';
import TaskNode from './components/TaskNode';
import MetricsPanel from './components/MetricsPanel';

export default function App() {
    // 3. ZUSTAND BAĞLANTISI: Verileri hook'tan değil, doğrudan global depodan çekiyoruz!
    const tasks = useSimulationStore((state) => state.tasks);
    const metrics = useSimulationStore((state) => state.metrics);

    // WebSocket hook'unu sadece bağlantıyı kursun diye çağırıyoruz 
    // (Artık state döndürmesine gerek yok, arka planda store'u güncelleyecek)
    useWebSocket(); 

    const [nodes, setNodes] = useState([]);
    const nodeTypes = useMemo(() => ({ custom: TaskNode }), []);

    const handleCreateTask = async (type, priority) => {
        try {
            await createTask(type, priority);
        } catch (error) {
            console.error("Task oluşturulamadı:", error);
        }
    };

    const handleClearAll = async () => {
        try {
            await clearAllTasks();
        } catch (error) {
            console.error("Silme işlemi başarısız:", error);
        }
    };

    // React Flow Node'larını Ayarlama
    useEffect(() => {
        const waitingTasks = tasks.filter(t => t.status === 'WAITING');
        const runningTasks = tasks.filter(t => t.status === 'RUNNING' || t.status === 'BLOCKED');
        const completedTasks = tasks.filter(t => ['SUCCESS', 'FAILED', 'TIMEOUT', 'CANCELLED'].includes(t.status));

        const createNode = (task, index, xOffset) => ({
            id: task.id.toString(),
            type: 'custom',
            position: { x: xOffset, y: index * 140 + 50 },
            data: { task: task }
        });

        const newNodes = [
            ...waitingTasks.map((t, i) => createNode(t, i, 50)),
            ...runningTasks.map((t, i) => createNode(t, i, 350)),
            ...completedTasks.slice(-10).map((t, i) => createNode(t, i, 650))
        ];

        setNodes(newNodes);
    }, [tasks]);

    // MiniMap renk mantığını dışarı çıkardık (Clean Code)
    const getNodeColor = (node) => {
        switch (node.data?.task?.status) {
            case 'RUNNING': return '#3b82f6'; 
            case 'BLOCKED': return '#9333ea'; 
            case 'SUCCESS': return '#10b981'; 
            case 'FAILED': return '#ef4444';  
            case 'CANCELLED': return '#f59e0b';
            case 'TIMEOUT': return '#ec4899'; 
            default: return '#94a3b8'; 
        }
    };

    return (
        <div className="app-container">
            {/* ÜST PANEL */}
            <div className="top-panel">
                <div className="panel-left">
                    <div className="header-section">
                        <h2 className="app-title">DispatchSim Console</h2>
                        <button className="modern-btn btn-dark" onClick={handleClearAll}>
                            🗑️ Temizle
                        </button>
                    </div>

                    <div className="button-groups-wrapper">
                        {/* Grup 1: Temel Görevler */}
                        <div className="btn-group">
                            <button className="modern-btn btn-blue" onClick={() => handleCreateTask('CPU_BOUND', 'LOW')}>CPU (Low)</button>
                            <button className="modern-btn btn-red" onClick={() => handleCreateTask('CPU_BOUND', 'CRITICAL')}>CPU (Crit)</button>
                            <button className="modern-btn btn-purple" onClick={() => handleCreateTask('IO_BOUND', 'MEDIUM')}>IO (Med)</button>
                        </div>

                        {/* Grup 2: Senaryolar */}
                        <div className="btn-group">
                            <button className="modern-btn btn-deadlock" onClick={triggerDeadlock}>Deadlock</button>
                            <button className="modern-btn btn-starvation" onClick={triggerStarvation}>Starvation</button>
                            <button className="modern-btn btn-circuit" onClick={triggerCircuitBreaker}>Şalter</button>
                            <button className="modern-btn btn-timeout" onClick={triggerTimeout}>Timeout</button>
                            <button className="modern-btn btn-memory" onClick={triggerMemoryLeak}>Mem Leak</button>
                        </div>

                        {/* Grup 3: Modern Mimariler */}
                        <div className="btn-group no-border">
                            <button className="modern-btn btn-platform" onClick={() => triggerLoom(false)}>Platform (50)</button>
                            <button className="modern-btn btn-virtual" onClick={() => triggerLoom(true)}>Virtual (50)</button>
                            <button className="modern-btn btn-autoscale" onClick={triggerAutoScale}>Auto-Scale</button>
                        </div>
                    </div>
                </div>

                <div className="panel-right">
                    <MetricsPanel metrics={metrics} />
                </div>
            </div>

            {/* REACT FLOW TUVALİ */}
            <div className="flow-container">
                <ReactFlow nodes={nodes} edges={[]} nodeTypes={nodeTypes}>
                    <Background color="#cbd5e1" gap={20} size={2} />
                    <Controls />
                    <MiniMap nodeColor={getNodeColor} />
                </ReactFlow>
            </div>
        </div>
    );
}