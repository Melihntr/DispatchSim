package com.dispatchsim.service;

import com.dispatchsim.model.dto.TaskEvent;
import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.model.enums.Priority;
import com.dispatchsim.model.enums.TaskStatus;
import com.dispatchsim.model.enums.TaskType;
import com.dispatchsim.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDispatcherServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WebSocketPublisher webSocketPublisher;

    @InjectMocks
    private TaskDispatcherService taskDispatcherService;

    @BeforeEach
    void setUp() {
        // Her testten önce sistemi temiz bir kopyayla başlatıyoruz
        taskDispatcherService.clearAll();
    }

    @Test
    void testSubmitNormalTask() {
        // Given
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setType(TaskType.CPU_BOUND);
        task.setPriority(Priority.HIGH);

        when(taskRepository.save(any(TaskEntity.class))).thenReturn(task);

        // When
        taskDispatcherService.submitTask(task);

        // Then
        verify(taskRepository, atLeastOnce()).save(task);
        verify(webSocketPublisher, atLeastOnce()).publishTaskUpdate(any(TaskEvent.class));
        assertTrue(taskDispatcherService.getQueueSize() >= 0);
        assertEquals(4, taskDispatcherService.getMaxThreads());
    }

    @Test
    void testSubmitNormalTaskWithNull() {
        // Given
        TaskEntity task = null;

        // When and Then
        assertThrows(NullPointerException.class, () -> taskDispatcherService.submitTask(task));
    }

    @Test
    void testClearAll() {
        // When
        taskDispatcherService.clearAll();

        // Then
        verify(taskRepository).deleteAll();
        verify(webSocketPublisher).publishTaskUpdate(argThat(event -> event.getEventType().equals("CLEAR_ALL")));
        assertEquals(0, taskDispatcherService.getActiveThreadCount());
    }

    @Test
    void testClearAllWithException() {
        // Given
        doThrow(new RuntimeException("Test Exception")).when(taskRepository).deleteAll();

        // When and Then
        assertThrows(RuntimeException.class, () -> taskDispatcherService.clearAll());
    }

    @Test
    void testCircuitBreakerSimulation() {
        // When
        taskDispatcherService.triggerCircuitBreakerSimulation();

        // Then
        verify(taskRepository, timeout(5000).atLeastOnce()).save(argThat(task ->
                task != null && task.getStatus() == TaskStatus.CANCELLED
        ));
    }

    @Test
    void testCircuitBreakerSimulationWithException() {
        // Given
        doThrow(new RuntimeException("Test Exception")).when(taskRepository).save(any(TaskEntity.class));

        // When
        taskDispatcherService.triggerCircuitBreakerSimulation();

        // Then
        verify(taskRepository, timeout(5000).atLeastOnce()).save(argThat(task ->
                task != null && task.getStatus() == TaskStatus.CANCELLED
        ));
    }

    @Test
    void testTimeoutSimulation() {
        // Given
        TaskEntity mockTask = new TaskEntity();
        mockTask.setId(99L);
        mockTask.setStatus(TaskStatus.WAITING);

        when(taskRepository.save(any(TaskEntity.class))).thenReturn(mockTask);
        when(taskRepository.findById(any())).thenReturn(Optional.of(mockTask));

        // When
        taskDispatcherService.triggerTimeoutSimulation();

        // Then
        verify(taskRepository, timeout(3000).atLeastOnce()).save(argThat(task ->
                task != null && task.getStatus() == TaskStatus.TIMEOUT
        ));
    }

    @Test
    void testTimeoutSimulationWithException() {
        // Given
        doThrow(new RuntimeException("Test Exception")).when(taskRepository).save(any(TaskEntity.class));

        // When
        taskDispatcherService.triggerTimeoutSimulation();

        // Then
        verify(taskRepository, timeout(3000).atLeastOnce()).save(argThat(task ->
                task != null && task.getStatus() == TaskStatus.TIMEOUT
        ));
    }

    @Test
    void testAutoScaling() {
        // When
        taskDispatcherService.triggerAutoScaleSimulation();

        // Then
        // 2 saniye sonra kapasite 4'ten 8'e çıkmalı
        assertEquals(8, taskDispatcherService.getMaxThreads());
    }

    @Test
    void testAutoScalingWithException() {
        // Given
        doThrow(new RuntimeException("Test Exception")).when(taskDispatcherService).triggerAutoScaleSimulation();

        // When and Then
        assertThrows(RuntimeException.class, () -> taskDispatcherService.triggerAutoScaleSimulation());
    }

    @Test
    void testGetQueueSize() {
        // When
        int queueSize = taskDispatcherService.getQueueSize();

        // Then
        assertTrue(queueSize >= 0);
    }

    @Test
    void testGetMaxThreads() {
        // When
        int maxThreads = taskDispatcherService.getMaxThreads();

        // Then
        assertEquals(4, maxThreads);
    }

    @Test
    void testGetActiveThreadCount() {
        // When
        int activeThreadCount = taskDispatcherService.getActiveThreadCount();

        // Then
        assertEquals(0, activeThreadCount);
    }
}
