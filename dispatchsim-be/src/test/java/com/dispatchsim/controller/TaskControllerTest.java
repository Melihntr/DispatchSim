package com.dispatchsim.controller;

import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.service.TaskDispatcherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskDispatcherService taskDispatcherService;

    @Test
    void testCreateTask() throws Exception {
        doNothing().when(taskDispatcherService).submitTask(any(TaskEntity.class));

        mockMvc.perform(post("/api/tasks")
                        .param("type", "CPU_BOUND")
                        .param("priority", "HIGH")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(taskDispatcherService).submitTask(any(TaskEntity.class));
    }

    @Test
    void testClearAllTasks() throws Exception {
        doNothing().when(taskDispatcherService).clearAll();

        mockMvc.perform(delete("/api/tasks"))
                .andExpect(status().isOk());

        verify(taskDispatcherService).clearAll();
    }

    @Test
    void testTriggerSimulations() throws Exception {
        // Tüm endpointlerin 200 OK döndüğünü test ediyoruz
        mockMvc.perform(post("/api/tasks/deadlock")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/starvation")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/circuit-breaker")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/timeout")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/memory-leak")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/autoscale")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/loom").param("virtual", "true")).andExpect(status().isOk());

        // Servislerin çağrıldığını doğrula
        verify(taskDispatcherService).triggerDeadlockSimulation();
        verify(taskDispatcherService).triggerStarvationSimulation();
        verify(taskDispatcherService).triggerCircuitBreakerSimulation();
        verify(taskDispatcherService).triggerTimeoutSimulation();
        verify(taskDispatcherService).triggerMemoryLeakSimulation();
        verify(taskDispatcherService).triggerAutoScaleSimulation();
        verify(taskDispatcherService).triggerLoomSimulation(true);
    }
}