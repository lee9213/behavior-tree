package com.lee9213.behavior.engine.retry;

import java.util.concurrent.ThreadLocalRandom;

public final class RetryExecutor {

    public interface RunnableAttempt<T> {
        T run() throws Exception;
    }

    private RetryExecutor() {
    }

    public static <T> T execute(boolean retryEnabled, RetryPolicy policy, RunnableAttempt<T> attempt) throws Exception {
        if (!retryEnabled) {
            return attempt.run();
        }
        int attemptNo = 0;
        long delay = policy.baseDelayMillis();
        Exception last = null;
        while (attemptNo < policy.maxAttempts()) {
            try {
                return attempt.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception ex) {
                last = ex;
                attemptNo++;
                if (attemptNo >= policy.maxAttempts()) {
                    break;
                }
                long jitter = (long) (delay * policy.jitterRatio() * ThreadLocalRandom.current().nextDouble());
                try {
                    Thread.sleep(delay + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ie;
                }
                delay = (long) (delay * policy.multiplier());
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("retry without exception");
    }
}
