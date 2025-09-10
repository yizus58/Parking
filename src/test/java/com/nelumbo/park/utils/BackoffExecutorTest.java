package com.nelumbo.park.utils;

import com.nelumbo.park.exception.exceptions.BackoffExecutionFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackoffExecutorTest {

    @Mock
    private Function<String, Boolean> mockFunction;

    @Mock
    private BiConsumer<Exception, String> mockOnFinalError;

    @Mock
    private BiConsumer<Boolean, String> mockOnSuccess;

    @Mock
    private BiConsumer<Exception, String> mockOnRetry;

    @Test
    @DisplayName("Should succeed on the first attempt when function returns true")
    void executeBackoff_SuccessFirstAttempt() {
        // Given
        when(mockFunction.apply("input")).thenReturn(true);
        BackoffExecutor<String> executor = new BackoffExecutor<>(10, 3, mockFunction, mockOnFinalError, mockOnSuccess, mockOnRetry);

        // When & Then
        assertDoesNotThrow(() -> executor.executeBackoff("input"));

        // Verify
        verify(mockFunction, times(1)).apply("input");
        verify(mockOnSuccess, times(1)).accept(true, "input");
        verify(mockOnRetry, never()).accept(any(), any());
        verify(mockOnFinalError, never()).accept(any(), any());
    }

    @Test
    @DisplayName("Should succeed after one retry when function returns false then true")
    void executeBackoff_SuccessAfterOneFalseRetry() {
        // Given
        when(mockFunction.apply("input")).thenReturn(false, true);
        BackoffExecutor<String> executor = new BackoffExecutor<>(10, 3, mockFunction, mockOnFinalError, mockOnSuccess, mockOnRetry);

        // When & Then
        assertDoesNotThrow(() -> executor.executeBackoff("input"));

        // Verify
        verify(mockFunction, times(2)).apply("input");
        verify(mockOnSuccess, times(1)).accept(true, "input");
        verify(mockOnRetry, times(1)).accept(any(Exception.class), eq("input"));
        verify(mockOnFinalError, never()).accept(any(), any());
    }

    @Test
    @DisplayName("Should succeed after one retry when function throws exception then returns true")
    void executeBackoff_SuccessAfterOneExceptionRetry() {
        // Given
        RuntimeException testException = new RuntimeException("Test Exception");
        when(mockFunction.apply("input")).thenThrow(testException).thenReturn(true);
        BackoffExecutor<String> executor = new BackoffExecutor<>(10, 3, mockFunction, mockOnFinalError, mockOnSuccess, mockOnRetry);

        // When & Then
        assertDoesNotThrow(() -> executor.executeBackoff("input"));

        // Verify
        verify(mockFunction, times(2)).apply("input");
        verify(mockOnSuccess, times(1)).accept(true, "input");
        verify(mockOnRetry, times(1)).accept(eq(testException), eq("input"));
        verify(mockOnFinalError, never()).accept(any(), any());
    }

    @Test
    @DisplayName("Should fail after all retries when function always returns false")
    void executeBackoff_FailAfterAllRetries_AlwaysReturnsFalse() {
        // Given
        when(mockFunction.apply("input")).thenReturn(false);
        BackoffExecutor<String> executor = new BackoffExecutor<>(10, 2, mockFunction, mockOnFinalError, mockOnSuccess, mockOnRetry);

        // When & Then
        assertThrows(BackoffExecutionFailedException.class, () -> executor.executeBackoff("input"));

        // Verify (1 initial attempt + 2 retries = 3 calls)
        verify(mockFunction, times(3)).apply("input");
        verify(mockOnSuccess, never()).accept(any(), any());
        verify(mockOnRetry, times(3)).accept(any(Exception.class), eq("input"));
        verify(mockOnFinalError, times(1)).accept(any(Exception.class), eq("input"));
    }

    @Test
    @DisplayName("Should fail after all retries when function always throws exception")
    void executeBackoff_FailAfterAllRetries_AlwaysThrowsException() {
        // Given
        RuntimeException testException = new RuntimeException("Test Exception");
        when(mockFunction.apply("input")).thenThrow(testException);
        BackoffExecutor<String> executor = new BackoffExecutor<>(10, 2, mockFunction, mockOnFinalError, mockOnSuccess, mockOnRetry);

        // When & Then
        assertThrows(BackoffExecutionFailedException.class, () -> executor.executeBackoff("input"));

        // Verify (1 initial attempt + 2 retries = 3 calls)
        verify(mockFunction, times(3)).apply("input");
        verify(mockOnSuccess, never()).accept(any(), any());
        verify(mockOnRetry, times(3)).accept(eq(testException), eq("input"));
        verify(mockOnFinalError, times(1)).accept(eq(testException), eq("input"));
    }
}
