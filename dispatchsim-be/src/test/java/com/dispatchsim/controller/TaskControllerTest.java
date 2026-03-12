package com.dispatchsim.controller;

import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.repository.TaskRepository;
import com.dispatchsim.service.TaskDispatcherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskDispatcherService taskDispatcherService;

    // EKSİK PARÇA: Controller'da Repo kullandığın için bunu da mockluyoruz
    @MockBean
    private TaskRepository taskRepository;

    @Test
    void testCreateTask() throws Exception {
        when(taskDispatcherService.submitTask(any(TaskEntity.class))).thenReturn(new TaskEntity());

        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"CPU_BOUND\", \"priority\":\"HIGH\"}"))
                .andExpect(status().isOk());
    }

    // İŞTE %100'Ü GETİRECEK O KAYIP TEST!
    @Test
    void testGetAllTasks() throws Exception {
        when(taskRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk());
    }

    @Test
    void testClearAllTasks() throws Exception {
        // Senin koduna göre URL "/api/tasks/clear" olarak düzenlendi
        mockMvc.perform(delete("/api/tasks/clear")) 
                .andExpect(status().isOk());
    }

    @Test
    void testTriggerSimulations() throws Exception {
        mockMvc.perform(post("/api/tasks/deadlock")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/starvation")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/circuit-breaker")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/timeout")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/memory-leak")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/autoscale")).andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks/loom").param("virtual", "true")).andExpect(status().isOk());
    }
}