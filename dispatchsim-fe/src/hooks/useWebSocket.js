import { useEffect } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { fetchAllTasks } from '../services/api';
import { useSimulationStore } from '../store/useSimulationStore'; // Zustand depomuz

const SOCKET_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws-dispatch';

export const useWebSocket = () => {
    // 1. ZUSTAND FONKSİYONLARINI ÇEKİYORUZ (useState yerine)
    const updateTask = useSimulationStore((state) => state.updateTask);
    const updateMetrics = useSimulationStore((state) => state.updateMetrics);
    const setInitialTasks = useSimulationStore((state) => state.setInitialTasks);
    const setConnectionStatus = useSimulationStore((state) => state.setConnectionStatus);

    // 2. İLK AÇILIŞTA GEÇMİŞ VERİLERİ (REST API) ÇEKME
    useEffect(() => {
        const loadInitialTasks = async () => {
            try {
                const initialData = await fetchAllTasks();
                setInitialTasks(initialData); // Doğrudan Zustand'a basıyoruz
            } catch (error) {
                console.error("Başlangıç verileri çekilemedi:", error);
            }
        };
        loadInitialTasks();
    }, []);

    // 3. WEBSOCKET (STOMP) BAĞLANTISI
    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(SOCKET_URL),
            reconnectDelay: 5000,
            
            // 🚀 EFSANE DOKUNUŞ: HEARTBEAT (KALP ATIŞI)
            // Tarayıcı ve Sunucu birbirlerine 10 saniyede bir "Buradayım" der.
            // Bu sayede bağlantı saatlerce açık kalsa bile kopmaz (Firewall kesmez).
            heartbeatIncoming: 10000, 
            heartbeatOutgoing: 10000,

            onConnect: () => {
                console.log('✅ WebSocket Bağlantısı Kuruldu!');
                setConnectionStatus(true);

                // GÖREV KANALI
                client.subscribe('/topic/tasks', (message) => {
                    if (message.body) {
                        const event = JSON.parse(message.body);
                        // Eskiden burada 15 satır if/else if vardı. 
                        // Artık o işi Zustand yapıyor. Sadece paketi fırlatıyoruz!
                        updateTask(event); 
                    }
                });

                // METRİK KANALI
                client.subscribe('/topic/metrics', (message) => {
                    if (message.body) {
                        const metricsData = JSON.parse(message.body);
                        updateMetrics(metricsData);
                    }
                });
            },
            onStompError: (frame) => {
                console.error('❌ Broker hatası: ' + frame.headers['message']);
                setConnectionStatus(false);
            },
            onWebSocketClose: () => {
                console.log('⚠️ WebSocket Bağlantısı Koptu!');
                setConnectionStatus(false);
            }
        });

        // Motoru çalıştır
        client.activate();

        // Component silinirse tüneli güvenli bir şekilde kapat (Memory Leak önlemi)
        return () => {
            if (client.active) {
                client.deactivate();
            }
        };
    }, []); 

    // ❌ ESKİSİ GİBİ return { tasks, metrics } YAPMIYORUZ!
    // Çünkü React bileşenleri artık veriyi bu hook'tan değil, doğrudan Zustand'dan çekecek.
};