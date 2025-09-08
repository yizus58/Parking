package com.nelumbo.park.utils;

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

    public void execute(T input) throws Exception {
        Exception lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                boolean result = fn.apply(input);
                if (onSuccess != null) onSuccess.accept(result, input);
                return;
            } catch (Exception e) {
                lastError = e;
                if (attempt == maxRetries) {
                    if (onFinalError != null) onFinalError.accept(e, input);
                    throw e;
                }
                if (onRetry != null) onRetry.accept(e, input);
                long delay = baseDelay * (1L << attempt);
                Thread.sleep(delay);
            }
        }
        throw lastError;
    }
}
