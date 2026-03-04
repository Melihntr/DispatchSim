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