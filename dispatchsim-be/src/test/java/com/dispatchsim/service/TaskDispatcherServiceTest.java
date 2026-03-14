package com.dispatchsim.service;

import com.dispatchsim.model.dto.TaskEvent;
import com.dispatchsim.model.entity.TaskEntity;
import com.dispatchsim.model.enums.Priority;
import com.dispatchsim.model.enums.TaskStatus;
import com.dispatchsim.model.enums.TaskType;
import com.dispatchsim.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TaskDispatcherServiceTest {

    private TaskRepository taskRepository;
    private WebSocketPublisher webSocketPublisher;
    private ThreadPoolExecutor executor;

    private TaskDispatcherService service;

    @BeforeEach
    void setup() {
        taskRepository = Mockito.mock(TaskRepository.class);
        webSocketPublisher = Mockito.mock(WebSocketPublisher.class);

        executor = new ThreadPoolExecutor(
                2,
                4,
                60,
                TimeUnit.SECONDS,
                new PriorityBlockingQueue<>(100)
        );

        service = new TaskDispatcherService(executor, taskRepository, webSocketPublisher);

        when(taskRepository.save(any())).thenAnswer(invocation -> {
            TaskEntity t = invocation.getArgument(0);

            if (t.getCreatedAt() == null) {
                t.setCreatedAt(java.time.LocalDateTime.now());
            }

            return t;
        });
    }

    @AfterEach
    void cleanup() {
        executor.shutdownNow();
    }

    @Test
    void testSubmitTask() throws Exception {
        TaskEntity task = new TaskEntity();
        task.setPriority(Priority.HIGH);
        task.setType(TaskType.CPU_BOUND);

        service.submitTask(task);

        Thread.sleep(3500);

        verify(taskRepository, atLeastOnce()).save(any());
        verify(webSocketPublisher, atLeastOnce()).publishTaskUpdate(any(TaskEvent.class));
    }

    @Test
    void testClearAll() {

        service.clearAll();

        verify(taskRepository).deleteAll();
        verify(webSocketPublisher).publishTaskUpdate(any());
    }

    @Test
    void testDeadlockSimulation() throws Exception {

        service.triggerDeadlockSimulation();

        Thread.sleep(2000);

        verify(taskRepository, atLeastOnce()).save(any());
        verify(webSocketPublisher, atLeastOnce()).publishTaskUpdate(any());
    }

    @Test
    void testStarvationSimulation() throws Exception {

        service.triggerStarvationSimulation();

        Thread.sleep(4000);

        verify(taskRepository, atLeastOnce()).save(any());
    }

    @Test
    void testLoomSimulationPlatformThreads() throws Exception {

        service.triggerLoomSimulation(false);

        Thread.sleep(3000);

        verify(taskRepository, atLeastOnce()).save(any());
    }

    @Test
    void testLoomSimulationVirtualThreads() throws Exception {

        service.triggerLoomSimulation(true);

        Thread.sleep(3000);

        verify(taskRepository, atLeastOnce()).save(any());
    }

    @Test
    void testCircuitBreakerSimulation() throws Exception {

        service.triggerCircuitBreakerSimulation();

        Thread.sleep(10000);

        verify(taskRepository, atLeastOnce()).save(any());
    }

    @Test
    void testTimeoutSimulation() throws Exception {

        service.triggerTimeoutSimulation();

        Thread.sleep(4000);

        verify(taskRepository, atLeastOnce()).save(any());
    }

    @Test
    void testAutoScaleSimulation() throws Exception {

        service.triggerAutoScaleSimulation();

        Thread.sleep(5000);

        verify(taskRepository, atLeastOnce()).save(any());
    }

    @Test
    void testMemoryLeakSimulation() throws Exception {

        service.triggerMemoryLeakSimulation();

        Thread.sleep(2000);
    }

    @Test
    void testMetricsMethods() {

        int active = service.getActiveThreadCount();
        int queue = service.getQueueSize();
        int max = service.getMaxThreads();

        assert max >= 0;
        assert active >= 0;
        assert queue >= 0;
    }

    // -------- PRIVATE METHOD COVERAGE --------

    @Test
    void testCheckCircuitBreakerReflection() throws Exception {

        Method method = TaskDispatcherService.class.getDeclaredMethod("checkCircuitBreaker");
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(service);

        assert result;
    }

    @Test
    void testRecordFailureReflection() throws Exception {

        Method method = TaskDispatcherService.class.getDeclaredMethod("recordFailure");
        method.setAccessible(true);

        method.invoke(service);
        method.invoke(service);
        method.invoke(service);
    }

    @Test
    void testRecordSuccessReflection() throws Exception {

        Method method = TaskDispatcherService.class.getDeclaredMethod("recordSuccess");
        method.setAccessible(true);

        method.invoke(service);
    }

    @Test
    void testSubmitSimulatedTaskReflection() throws Exception {

        Method method = TaskDispatcherService.class.getDeclaredMethod("submitSimulatedTask", boolean.class);
        method.setAccessible(true);

        method.invoke(service, true);
        method.invoke(service, false);

        Thread.sleep(2000);

        verify(taskRepository, atLeastOnce()).save(any());
    }
}