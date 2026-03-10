import React, { useEffect, useState, useMemo } from 'react';
import ReactFlow, { Background, Controls, MiniMap } from 'reactflow';
import 'reactflow/dist/style.css';
 
import { useWebSocket } from './hooks/useWebSocket.js';
// BÜTÜN API FONKSİYONLARINI BURADA İÇERİ ALIYORUZ
import {
    createTask,
    clearAllTasks,
    triggerDeadlock,
    triggerStarvation,
    triggerCircuitBreaker,
    triggerTimeout,
    triggerLoom,
    triggerAutoScale,
    triggerMemoryLeak
} from './services/api';
import TaskNode from './components/TaskNode';
import MetricsPanel from './components/MetricsPanel';
 
export default function App() {
    const { tasks, metrics } = useWebSocket();
    const [nodes, setNodes] = useState([]);
 
    const nodeTypes = useMemo(() => ({ custom: TaskNode }), []);
 
    // GÖREV OLUŞTURMA FONKSİYONU
    const handleCreateTask = async (type, priority) => {
        try {
            await createTask(type, priority);
        } catch (error) {
            console.error("Task oluşturulamadı:", error);
        }
    };
 
    // TEMİZLEME FONKSİYONU (Hatanın sebebi buranın veya importun eksik olmasıydı)
    const handleClearAll = async () => {
        try {
            await clearAllTasks();
            console.log("Silme isteği backend'e gitti!");
        } catch (error) {
            console.error("Silme işlemi başarısız:", error);
        }
    };
 
    // REACT FLOW NODE'LARINI AYARLAMA
    useEffect(() => {
        const waitingTasks = tasks.filter(t => t.status === 'WAITING');
        // Kilitlenenleri de orta sütunda (çalışma havuzunda) gösteriyoruz
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
 
    return (
        <div style={{ height: '100vh', width: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            
            {/* Üst Panel - KOMPAKT TASARIM */}
            <div style={{ padding: '12px 20px', background: '#0f172a', color: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', boxShadow: '0 4px 6px rgba(0,0,0,0.3)', zIndex: 10 }}>
 
                {/* Sol Taraf: Başlık ve Butonlar */}
                <div style={{ flex: 1, marginRight: '20px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                        <h2 style={{ margin: 0, fontSize: '22px', fontWeight: '650', letterSpacing: '1px' }}>
                            DispatchSim Console
                        </h2>
                        {/* Temizle Butonunu yukarı başlığın yanına aldık */}
                        <button className="modern-btn btn-dark" style={{ padding: '6px 12px', fontSize: '12px' }} onClick={handleClearAll}>
                            🗑️ Temizle
                        </button>
                    </div>
 
                    {/* Buton Grupları - Flex Wrap ile iki katlı görünüm */}
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px' }}>
 
                        {/* Grup 1: Temel Görevler */}
                        <div style={{ display: 'flex', gap: '6px', paddingRight: '10px', borderRight: '1px solid #334155' }}>
                            <button className="modern-btn btn-blue" style={{ padding: '6px 10px', fontSize: '11px' }} onClick={() => handleCreateTask('CPU_BOUND', 'LOW')}>CPU (Low)</button>
                            <button className="modern-btn btn-red" style={{ padding: '6px 10px', fontSize: '11px' }} onClick={() => handleCreateTask('CPU_BOUND', 'CRITICAL')}>CPU (Crit)</button>
                            <button className="modern-btn btn-purple" style={{ padding: '6px 10px', fontSize: '11px' }} onClick={() => handleCreateTask('IO_BOUND', 'MEDIUM')}>IO (Med)</button>
                        </div>
 
                        {/* Grup 2: Senaryolar */}
                        <div style={{ display: 'flex', gap: '6px', paddingRight: '10px', borderRight: '1px solid #334155' }}>
                            <button className="modern-btn" style={{ background: '#7e22ce', padding: '6px 10px', fontSize: '11px' }} onClick={async () => await triggerDeadlock()}>Deadlock</button>
                            <button className="modern-btn" style={{ background: '#ea580c', padding: '6px 10px', fontSize: '11px' }} onClick={async () => await triggerStarvation()}>Starvation</button>
                            <button className="modern-btn" style={{ background: '#be123c', padding: '6px 10px', fontSize: '11px' }} onClick={async () => await triggerCircuitBreaker()}>Şalter</button>
                            <button className="modern-btn" style={{ background: '#ec4899', padding: '6px 10px', fontSize: '11px' }} onClick={async () => await triggerTimeout()}>Timeout</button>
                            <button className="modern-btn" style={{ background: '#854d0e', padding: '6px 10px', fontSize: '11px' }} onClick={async () => await triggerMemoryLeak()}>Mem Leak</button>
                        </div>
 
                        {/* Grup 3: Modern Mimariler */}
                        <div style={{ display: 'flex', gap: '6px' }}>
                            <button className="modern-btn" style={{ background: '#4b5563', padding: '6px 10px', fontSize: '11px' }} onClick={async () => await triggerLoom(false)}>Platform (50)</button>
                            <button className="modern-btn" style={{ background: '#059669', padding: '6px 10px', fontSize: '11px' }} onClick={async () => await triggerLoom(true)}>Virtual (50)</button>
                            <button className="modern-btn" style={{ background: '#0891b2', padding: '6px 10px', fontSize: '11px' }} onClick={async () => await triggerAutoScale()}>Auto-Scale</button>
                        </div>
                    </div>
                </div>
 
                {/* Sağ Taraf: Metrics Panel (Sabit ve sıkışmaz) */}
                <div style={{ flexShrink: 0 }}>
                    <MetricsPanel metrics={metrics} />
                </div>
            </div>
 
            {/* React Flow Tuvali */}
            <div style={{ flex: 1, position: 'relative', background: '#f8fafc' }}>
                <ReactFlow nodes={nodes} edges={[]} nodeTypes={nodeTypes}>
                    <Background color="#cbd5e1" gap={20} size={2} />
                    <Controls />
                    <MiniMap
                        nodeColor={(node) => {
                            switch (node.data?.task?.status) {
                                case 'RUNNING': return '#3b82f6'; // Mavi
                                case 'BLOCKED': return '#9333ea'; // Mor
                                case 'SUCCESS': return '#10b981'; // Yeşil
                                case 'FAILED': return '#ef4444';  // Kırmızı
                                case 'CANCELLED': return '#f59e0b'; // Turuncu
                                case 'TIMEOUT': return '#ec4899'; // Pembe
                                default: return '#94a3b8'; // Gri
                            }
                        }}
                    />
                </ReactFlow>
            </div>
        </div>
    );
}