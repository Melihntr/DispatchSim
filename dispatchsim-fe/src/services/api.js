import axios from 'axios';

const API_URL = 'http://localhost:8080/api/tasks';

export const fetchAllTasks = async () => {
    const response = await axios.get(API_URL);
    return response.data;
};

export const createTask = async (type, priority) => {
    const response = await axios.post(API_URL, { type, priority });
    return response.data;
};

export const clearAllTasks = async () => {
    await axios.delete(`${API_URL}/clear`);
};

export const triggerDeadlock = async () => {
    await axios.post(`${API_URL}/deadlock`);
};
export const triggerStarvation = async () => {
    await axios.post(`${API_URL}/starvation`);
};
export const triggerLoom = async (isVirtual) => {
    await axios.post(`${API_URL}/loom?virtual=${isVirtual}`);
};
export const triggerCircuitBreaker = async () => { await axios.post(`${API_URL}/circuit-breaker`); };
export const triggerTimeout = async () => { await axios.post(`${API_URL}/timeout`); };