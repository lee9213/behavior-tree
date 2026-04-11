package com.lee9213.behavior.engine.retry;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryExecutorTest {

    @Test
    void retryDisabled_onlyOneAttempt_whenThrows() {
        AtomicInteger attempts = new AtomicInteger();
        RetryPolicy policy = new RetryPolicy(5, 1L, 2.0, 0.0);
        assertThrows(RuntimeException.class, () -> RetryExecutor.execute(false, policy, () -> {
            attempts.incrementAndGet();
            throw new RuntimeException("fail");
        }));
        assertEquals(1, attempts.get());
    }
}
