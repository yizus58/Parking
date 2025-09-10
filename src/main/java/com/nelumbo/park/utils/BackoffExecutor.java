package com.nelumbo.park.utils;

import com.nelumbo.park.exception.exceptions.BackoffExecutionFailedException;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class BackoffExecutor<T> {
    private final int baseDelay;
    private final int maxRetries;
    private final Function<T, Boolean> fn;
    private final BiConsumer<Exception, T> onFinalError;
    private final BiConsumer<Boolean, T> onSuccess;
    private final BiConsumer<Exception, T> onRetry;

    public BackoffExecutor(int baseDelay, int maxRetries,
                           Function<T, Boolean> fn,
                           BiConsumer<Exception, T> onFinalError,
                           BiConsumer<Boolean, T> onSuccess,
                           BiConsumer<Exception, T> onRetry) {
        this.baseDelay = baseDelay;
        this.maxRetries = maxRetries;
        this.fn = fn;
        this.onFinalError = onFinalError;
        this.onSuccess = onSuccess;
        this.onRetry = onRetry;
    }

    public void executeBackoff(T input) throws BackoffExecutionFailedException {
        Exception lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (fn.apply(input)) {
                    if (onSuccess != null) {
                        onSuccess.accept(true, input);
                    }
                    return;
                }
                lastError = new Exception("Function returned false");
            } catch (Exception e) {
                lastError = e;
            }

            if (onRetry != null) {
                onRetry.accept(lastError, input);
            }

            if (attempt >= maxRetries) {
                handleFinalError(lastError, input);
            }

            waitWithBackoff(attempt);
        }
        throw new BackoffExecutionFailedException("La ejecución con backoff falló después de " + maxRetries + " intentos", lastError);
    }

    private void handleFinalError(Exception e, T input) throws BackoffExecutionFailedException {
        if (onFinalError != null) onFinalError.accept(e, input);
        throw new BackoffExecutionFailedException("La ejecución con backoff falló después de " + maxRetries + " intentos", e);
    }

    private void waitWithBackoff(int attempt) throws BackoffExecutionFailedException {
        if (attempt < maxRetries) {
            try {
                long delay = baseDelay * (1L << attempt);
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new BackoffExecutionFailedException("La ejecución con backoff fue interrumpida", ie);
            }
        }
    }
}
