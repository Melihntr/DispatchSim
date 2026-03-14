import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { fetchAllTasks } from '../services/api'; // API'yi import ettik

const SOCKET_URL = import.meta.env.VITE_WS_URL;

export const useWebSocket = () => {
    const [tasks, setTasks] = useState([]);
    const [metrics, setMetrics] = useState(null);

    // Sayfa ilk açıldığında geçmiş verileri çek
    useEffect(() => {
        const loadInitialTasks = async () => {
            try {
                const initialData = await fetchAllTasks();
                setTasks(initialData);
            } catch (error) {
                console.error("Başlangıç verileri çekilemedi:", error);
            }
        };
        loadInitialTasks();
    }, []);

    // WebSocket Bağlantısı
    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000,
            onConnect: () => {
                console.log('WebSocket Bağlantısı Kuruldu!');

                client.subscribe('/topic/tasks', (message) => {
                    const event = JSON.parse(message.body);

                    // 1. KONTROL: Eğer gelen komut CLEAR_ALL ise listeyi tamamen boşalt ve DUR
                    if (event.eventType === 'CLEAR_ALL') {
                        setTasks([]);
                        return;
                    }

                    // 2. GÜVENLİK DUVARI: Sadece event.task gerçekten varsa güncelleme yap
                    if (event.task && event.task.id != null) {
                        setTasks((prevTasks) => {
                            const existingTaskIndex = prevTasks.findIndex(t => t.id === event.task.id);
                            if (existingTaskIndex >= 0) {
                                const updatedTasks = [...prevTasks];
                                updatedTasks[existingTaskIndex] = event.task;
                                return updatedTasks;
                            } else {
                                return [...prevTasks, event.task];
                            }
                        });
                    }
                });

                client.subscribe('/topic/metrics', (message) => {
                    const metricsData = JSON.parse(message.body);
                    setMetrics(metricsData);
                });
            },
            onStompError: (frame) => {
                console.error('Broker hatası: ' + frame.headers['message']);
            },
        });

        client.activate();
        return () => client.deactivate();
    }, []);

    return { tasks, metrics };
};