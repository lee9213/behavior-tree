package com.lee9213.behavior.engine.action;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.engine.retry.RetryPolicy;
import com.lee9213.behavior.flow.FlowExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractRetryableActionNodeTest {

    @Test
    void retryDisabled_仅执行一次() {
        AtomicInteger attempts = new AtomicInteger();
        RetryPolicy policy = new RetryPolicy(5, 1L, 2.0, 0.0);
        AbstractRetryableActionNode node = new AbstractRetryableActionNode(false, policy) {
            @Override
            protected NodeResult doExecute(FlowExecutionContext context) {
                attempts.incrementAndGet();
                throw new IllegalStateException("fail");
            }
        };
        assertThrows(IllegalStateException.class, () -> node.execute(new FlowExecutionContext()));
        assertEquals(1, attempts.get());
    }

    @Test
    void retryEnabled_失败时按策略多次尝试() {
        AtomicInteger attempts = new AtomicInteger();
        RetryPolicy policy = new RetryPolicy(3, 1L, 2.0, 0.0);
        AbstractRetryableActionNode node = new AbstractRetryableActionNode(true, policy) {
            @Override
            protected NodeResult doExecute(FlowExecutionContext context) {
                attempts.incrementAndGet();
                throw new IllegalStateException("fail");
            }
        };
        assertThrows(Exception.class, () -> node.execute(new FlowExecutionContext()));
        assertEquals(3, attempts.get());
    }
}
