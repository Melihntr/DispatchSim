import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { fetchAllTasks } from '../services/api'; // API'yi import ettik

const SOCKET_URL = 'http://localhost:8080/ws-dispatch';

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