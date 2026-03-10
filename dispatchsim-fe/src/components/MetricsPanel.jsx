import React from 'react';
 
export default function MetricsPanel({ metrics }) {
    if (!metrics) return null;
 
    const heapPercent = Math.min((metrics.heapUsedMb / metrics.heapMaxMb) * 100, 100);
    const heapColor = heapPercent > 85 ? '#ef4444' : heapPercent > 60 ? '#f59e0b' : '#10b981';
 
    const maxThreads = metrics.maxThreads || 8;
    const threadPercent = Math.min((metrics.activeThreads / maxThreads) * 100, 100);
    const threadColor = threadPercent === 100 ? '#ef4444' : '#3b82f6';
 
    return (
        <div style={{
            background: '#1e293b',
            padding: '10px 16px',
            borderRadius: '8px',
            border: '1px solid #334155',
            // SİHİR BURADA: Dikeyden (Column) 2x2 yatay Grid'e geçtik!
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '8px 24px',
            boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.3)',
            alignItems: 'center'
        }}>
            {/* SOL ÜST: THREADS */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#cbd5e1' }}>
                    <span>Threads</span>
                    <span style={{ fontWeight: 'bold', color: 'white' }}>{metrics.activeThreads}/{maxThreads}</span>
                </div>
                <div style={{ width: '100%', background: '#0f172a', height: '6px', borderRadius: '3px', overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${threadPercent}%`, background: threadColor, transition: 'width 0.3s ease' }}></div>
                </div>
            </div>
 
            {/* SAĞ ÜST: HEAP MEMORY */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', color: '#cbd5e1' }}>
                    <span>Heap</span>
                    <span style={{ fontWeight: 'bold', color: 'white' }}>{metrics.heapUsedMb.toFixed(0)}/{metrics.heapMaxMb.toFixed(0)} MB</span>
                </div>
                <div style={{ width: '100%', background: '#0f172a', height: '6px', borderRadius: '3px', overflow: 'hidden' }}>
                    <div style={{ height: '100%', width: `${heapPercent}%`, background: heapColor, transition: 'width 0.5s ease' }}></div>
                </div>
            </div>
 
            {/* SOL ALT: QUEUE */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#0f172a', padding: '4px 8px', borderRadius: '4px', border: '1px solid #1e293b' }}>
                <span style={{ fontSize: '11px', color: '#cbd5e1' }}>Queue:</span>
                <span style={{
                    fontSize: '13px', fontWeight: '900',
                    color: metrics.queuedTasks > 10 ? '#ef4444' : metrics.queuedTasks > 0 ? '#f59e0b' : '#10b981',
                    textShadow: metrics.queuedTasks > 10 ? '0 0 8px rgba(239, 68, 68, 0.6)' : 'none'
                }}>
                    {metrics.queuedTasks}
                </span>
            </div>
 
            {/* SAĞ ALT: GARBAGE COLLECTION */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '10px', color: '#94a3b8', background: '#0f172a', padding: '5px 8px', borderRadius: '4px' }}>
                <span>GC: <strong style={{color: '#e2e8f0'}}>{metrics.gcPauseCount}x</strong></span>
                <span>Pause: <strong style={{color: '#e2e8f0'}}>{metrics.gcPauseTotalTimeMs ? metrics.gcPauseTotalTimeMs.toFixed(0) : 0}ms</strong></span>
            </div>
        </div>
    );
}