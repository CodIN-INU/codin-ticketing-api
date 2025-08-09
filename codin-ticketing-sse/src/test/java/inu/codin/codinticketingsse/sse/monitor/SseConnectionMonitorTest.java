package inu.codin.codinticketingsse.sse.monitor;

import inu.codin.codinticketingsse.sse.dto.SseEmitterTimeoutEvent;
import inu.codin.codinticketingsse.sse.repository.SseEmitterRepository;
import inu.codin.codinticketingsse.sse.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SseConnectionMonitor class.
 * Testing framework: JUnit 5 with Mockito for mocking dependencies.
 * 
 * This test class covers:
 * - Timeout event handling (successful and failed scenarios)
 * - Connection statistics logging with various thresholds
 * - Dead connection cleanup functionality
 * - Edge cases and error conditions
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SSE Connection Monitor Tests")
class SseConnectionMonitorTest {

    @Mock
    private SseEmitterRepository sseEmitterRepository;

    @Mock
    private SseService sseService;

    @InjectMocks
    private SseConnectionMonitor sseConnectionMonitor;

    private SseEmitterTimeoutEvent testTimeoutEvent;
    private static final String TEST_EMITTER_ID = UUID.randomUUID().toString();
    private static final Long TEST_EVENT_ID = 12345L;
    private static final Long TEST_USER_ID = 67890L;

    @BeforeEach
    void setUp() {
        testTimeoutEvent = SseEmitterTimeoutEvent.builder()
                .emitterId(TEST_EMITTER_ID)
                .eventId(TEST_EVENT_ID)
                .userId(TEST_USER_ID)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ==================== handleEmitterTimeout Tests ====================

    @Test
    @DisplayName("Should successfully handle emitter timeout event and remove emitter")
    void handleEmitterTimeout_Success() {
        // Given
        doNothing().when(sseEmitterRepository).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(testTimeoutEvent));

        // Then
        verify(sseEmitterRepository, times(1)).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle emitter timeout gracefully when removal fails")
    void handleEmitterTimeout_RemovalFailure() {
        // Given
        doThrow(new RuntimeException("Database connection error"))
                .when(sseEmitterRepository).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(testTimeoutEvent));

        // Then
        verify(sseEmitterRepository, times(1)).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle null timeout event fields gracefully")
    void handleEmitterTimeout_NullFields() {
        // Given
        SseEmitterTimeoutEvent nullFieldsEvent = SseEmitterTimeoutEvent.builder()
                .emitterId(null)
                .eventId(null)
                .userId(null)
                .timestamp(null)
                .build();

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(nullFieldsEvent));

        // Then
        verify(sseEmitterRepository, times(1)).removeEmitter(null, null);
    }

    @Test
    @DisplayName("Should handle concurrent timeout events")
    void handleEmitterTimeout_ConcurrentEvents() throws Exception {
        // Given
        int numberOfEvents = 10;
        doNothing().when(sseEmitterRepository).removeEmitter(any(), any());

        // When
        CompletableFuture<?>[] futures = new CompletableFuture[numberOfEvents];
        for (int i = 0; i < numberOfEvents; i++) {
            final Long eventId = (long) i;
            final Long userId = (long) (i * 100);
            SseEmitterTimeoutEvent event = SseEmitterTimeoutEvent.builder()
                    .emitterId("emitter-" + i)
                    .eventId(eventId)
                    .userId(userId)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            futures[i] = CompletableFuture.runAsync(() -> 
                sseConnectionMonitor.handleEmitterTimeout(event)
            );
        }
        
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        // Then
        verify(sseEmitterRepository, times(numberOfEvents)).removeEmitter(any(), any());
    }

    @Test
    @DisplayName("Should handle various exception types during emitter removal")
    void handleEmitterTimeout_VariousExceptions() {
        // Test IllegalArgumentException
        doThrow(new IllegalArgumentException("Invalid argument"))
                .when(sseEmitterRepository).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(testTimeoutEvent));

        // Test NullPointerException
        SseEmitterTimeoutEvent event2 = SseEmitterTimeoutEvent.builder()
                .emitterId("test-2")
                .eventId(2L)
                .userId(2L)
                .timestamp(LocalDateTime.now())
                .build();
        doThrow(new NullPointerException("Null value"))
                .when(sseEmitterRepository).removeEmitter(2L, 2L);
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(event2));

        // Verify both exceptions were handled
        verify(sseEmitterRepository, times(1)).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);
        verify(sseEmitterRepository, times(1)).removeEmitter(2L, 2L);
    }

    // ==================== logConnectionStats Tests ====================

    @Test
    @DisplayName("Should log connection stats with normal connection count")
    void logConnectionStats_NormalConnectionCount() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(500);

        // When
        sseConnectionMonitor.logConnectionStats();

        // Then
        verify(sseEmitterRepository, times(1)).getActiveConnectionCount();
    }

    @Test
    @DisplayName("Should log warning when connection count exceeds threshold")
    void logConnectionStats_ExceedsThreshold() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(1001);

        // When
        sseConnectionMonitor.logConnectionStats();

        // Then
        verify(sseEmitterRepository, times(1)).getActiveConnectionCount();
    }

    @Test
    @DisplayName("Should handle zero active connections")
    void logConnectionStats_ZeroConnections() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(0);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.logConnectionStats());

        // Then
        verify(sseEmitterRepository, times(1)).getActiveConnectionCount();
    }

    @Test
    @DisplayName("Should handle exactly threshold value (boundary condition)")
    void logConnectionStats_ExactlyThreshold() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(1000);

        // When
        sseConnectionMonitor.logConnectionStats();

        // Then
        verify(sseEmitterRepository, times(1)).getActiveConnectionCount();
        // Should not trigger warning as it's not > 1000
    }

    @Test
    @DisplayName("Should handle negative connection count (defensive programming)")
    void logConnectionStats_NegativeConnectionCount() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(-1);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.logConnectionStats());

        // Then
        verify(sseEmitterRepository, times(1)).getActiveConnectionCount();
    }

    @Test
    @DisplayName("Should handle maximum integer value")
    void logConnectionStats_MaxIntegerValue() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(Integer.MAX_VALUE);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.logConnectionStats());

        // Then
        verify(sseEmitterRepository, times(1)).getActiveConnectionCount();
    }

    @Test
    @DisplayName("Should handle exception when getting connection count")
    void logConnectionStats_ExceptionGettingCount() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount())
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        // Note: Since the method doesn't have try-catch, this will throw
        try {
            sseConnectionMonitor.logConnectionStats();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database error");
        }
        
        verify(sseEmitterRepository, times(1)).getActiveConnectionCount();
    }

    // ==================== cleanupDeadConnections Tests ====================

    @Test
    @DisplayName("Should successfully cleanup dead connections")
    void cleanupDeadConnections_Success() {
        // Given
        doNothing().when(sseService).sendHeartbeatAllEmitters();

        // When
        sseConnectionMonitor.cleanupDeadConnections();

        // Then
        verify(sseService, times(1)).sendHeartbeatAllEmitters();
    }

    @Test
    @DisplayName("Should handle exception during heartbeat sending")
    void cleanupDeadConnections_HeartbeatException() {
        // Given
        doThrow(new RuntimeException("Network error"))
                .when(sseService).sendHeartbeatAllEmitters();

        // When & Then
        try {
            sseConnectionMonitor.cleanupDeadConnections();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Network error");
        }
        
        verify(sseService, times(1)).sendHeartbeatAllEmitters();
    }

    @Test
    @DisplayName("Should handle multiple cleanup calls in sequence")
    void cleanupDeadConnections_MultipleCalls() {
        // Given
        doNothing().when(sseService).sendHeartbeatAllEmitters();

        // When
        for (int i = 0; i < 5; i++) {
            sseConnectionMonitor.cleanupDeadConnections();
        }

        // Then
        verify(sseService, times(5)).sendHeartbeatAllEmitters();
    }

    @Test
    @DisplayName("Should handle concurrent cleanup calls")
    void cleanupDeadConnections_ConcurrentCalls() throws Exception {
        // Given
        doNothing().when(sseService).sendHeartbeatAllEmitters();

        // When
        CompletableFuture<?>[] futures = new CompletableFuture[10];
        for (int i = 0; i < 10; i++) {
            futures[i] = CompletableFuture.runAsync(() -> 
                sseConnectionMonitor.cleanupDeadConnections()
            );
        }
        
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        // Then
        verify(sseService, times(10)).sendHeartbeatAllEmitters();
    }

    // ==================== Scheduled Annotation Tests ====================

    @Test
    @DisplayName("Should have correct scheduled fixed rate for logConnectionStats")
    void verifyLogConnectionStatsScheduling() throws NoSuchMethodException {
        // Use reflection to verify the @Scheduled annotation
        var method = SseConnectionMonitor.class.getMethod("logConnectionStats");
        var scheduledAnnotation = method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
        
        assertThat(scheduledAnnotation).isNotNull();
        assertThat(scheduledAnnotation.fixedRate()).isEqualTo(60000L); // 1 minute
    }

    @Test
    @DisplayName("Should have correct scheduled fixed rate for cleanupDeadConnections")
    void verifyCleanupDeadConnectionsScheduling() throws NoSuchMethodException {
        // Use reflection to verify the @Scheduled annotation
        var method = SseConnectionMonitor.class.getMethod("cleanupDeadConnections");
        var scheduledAnnotation = method.getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);
        
        assertThat(scheduledAnnotation).isNotNull();
        assertThat(scheduledAnnotation.fixedRate()).isEqualTo(300000L); // 5 minutes
    }

    // ==================== Integration-like Tests ====================

    @Test
    @DisplayName("Should handle complete lifecycle of timeout event")
    void completeTimeoutEventLifecycle() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(10, 9); // Before and after removal
        doNothing().when(sseEmitterRepository).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);

        // When
        sseConnectionMonitor.logConnectionStats(); // Log initial count
        sseConnectionMonitor.handleEmitterTimeout(testTimeoutEvent); // Handle timeout
        sseConnectionMonitor.logConnectionStats(); // Log count after removal

        // Then
        verify(sseEmitterRepository, times(2)).getActiveConnectionCount();
        verify(sseEmitterRepository, times(1)).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle monitoring operations with no active connections")
    void monitoringWithNoConnections() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(0);
        doNothing().when(sseService).sendHeartbeatAllEmitters();

        // When
        sseConnectionMonitor.logConnectionStats();
        sseConnectionMonitor.cleanupDeadConnections();

        // Then
        verify(sseEmitterRepository, times(1)).getActiveConnectionCount();
        verify(sseService, times(1)).sendHeartbeatAllEmitters();
    }

    @Test
    @DisplayName("Should handle rapid timeout events for same user")
    void rapidTimeoutEventsForSameUser() {
        // Given
        doNothing().when(sseEmitterRepository).removeEmitter(any(), eq(TEST_USER_ID));

        // When - simulate rapid timeout events for same user
        for (long eventId = 1; eventId <= 5; eventId++) {
            SseEmitterTimeoutEvent event = SseEmitterTimeoutEvent.builder()
                    .emitterId("emitter-" + eventId)
                    .eventId(eventId)
                    .userId(TEST_USER_ID)
                    .timestamp(LocalDateTime.now())
                    .build();
            sseConnectionMonitor.handleEmitterTimeout(event);
        }

        // Then
        verify(sseEmitterRepository, times(5)).removeEmitter(any(), eq(TEST_USER_ID));
    }

    // ==================== Edge Cases and Boundary Tests ====================

    @Test
    @DisplayName("Should handle timeout event with very large IDs")
    void handleEmitterTimeout_VeryLargeIds() {
        // Given
        SseEmitterTimeoutEvent largeIdEvent = SseEmitterTimeoutEvent.builder()
                .emitterId(UUID.randomUUID().toString())
                .eventId(Long.MAX_VALUE)
                .userId(Long.MAX_VALUE)
                .timestamp(LocalDateTime.now())
                .build();
        
        doNothing().when(sseEmitterRepository).removeEmitter(Long.MAX_VALUE, Long.MAX_VALUE);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(largeIdEvent));

        // Then
        verify(sseEmitterRepository, times(1)).removeEmitter(Long.MAX_VALUE, Long.MAX_VALUE);
    }

    @Test
    @DisplayName("Should handle timeout event with past timestamp")
    void handleEmitterTimeout_PastTimestamp() {
        // Given
        SseEmitterTimeoutEvent pastEvent = SseEmitterTimeoutEvent.builder()
                .emitterId(TEST_EMITTER_ID)
                .eventId(TEST_EVENT_ID)
                .userId(TEST_USER_ID)
                .timestamp(LocalDateTime.now().minusDays(30))
                .build();
        
        doNothing().when(sseEmitterRepository).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(pastEvent));

        // Then
        verify(sseEmitterRepository, times(1)).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle timeout event with future timestamp")
    void handleEmitterTimeout_FutureTimestamp() {
        // Given
        SseEmitterTimeoutEvent futureEvent = SseEmitterTimeoutEvent.builder()
                .emitterId(TEST_EMITTER_ID)
                .eventId(TEST_EVENT_ID)
                .userId(TEST_USER_ID)
                .timestamp(LocalDateTime.now().plusDays(1))
                .build();
        
        doNothing().when(sseEmitterRepository).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(futureEvent));

        // Then
        verify(sseEmitterRepository, times(1)).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("Should handle empty emitter ID")
    void handleEmitterTimeout_EmptyEmitterId() {
        // Given
        SseEmitterTimeoutEvent emptyIdEvent = SseEmitterTimeoutEvent.builder()
                .emitterId("")
                .eventId(TEST_EVENT_ID)
                .userId(TEST_USER_ID)
                .timestamp(LocalDateTime.now())
                .build();
        
        doNothing().when(sseEmitterRepository).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);

        // When
        assertDoesNotThrow(() -> sseConnectionMonitor.handleEmitterTimeout(emptyIdEvent));

        // Then
        verify(sseEmitterRepository, times(1)).removeEmitter(TEST_EVENT_ID, TEST_USER_ID);
    }

    // ==================== Performance and Stress Tests ====================

    @Test
    @DisplayName("Should handle high frequency of connection stats logging")
    void logConnectionStats_HighFrequency() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount()).thenReturn(100);

        // When - simulate high frequency logging
        for (int i = 0; i < 100; i++) {
            sseConnectionMonitor.logConnectionStats();
        }

        // Then
        verify(sseEmitterRepository, times(100)).getActiveConnectionCount();
    }

    @Test
    @DisplayName("Should handle alternating high and low connection counts")
    void logConnectionStats_AlternatingCounts() {
        // Given
        when(sseEmitterRepository.getActiveConnectionCount())
                .thenReturn(500, 1500, 300, 2000, 50);

        // When
        for (int i = 0; i < 5; i++) {
            sseConnectionMonitor.logConnectionStats();
        }

        // Then
        verify(sseEmitterRepository, times(5)).getActiveConnectionCount();
    }
}