package com.lee9213.behavior.action;

import com.lee9213.behavior.NodeResult;
import com.lee9213.behavior.flow.FlowExecutionContext;
import com.lee9213.behavior.tree.node.impl.AbstractActionNode;
import com.lee9213.behavior.tree.retry.RetryPolicy;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractActionNodeTest {

    @Test
    void retryDisabled_仅执行一次() {
        AtomicInteger attempts = new AtomicInteger();
        RetryPolicy policy = new RetryPolicy(5, 1L, 2.0, 0.0);
        AbstractActionNode<NodeResult, FlowExecutionContext> node = new AbstractActionNode<NodeResult, FlowExecutionContext>("test-node", false, policy) {
            @Override
            protected NodeResult doExecute(FlowExecutionContext context) throws Exception {
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
        AbstractActionNode<NodeResult, FlowExecutionContext> node = new AbstractActionNode<NodeResult, FlowExecutionContext>("test-node", true, policy) {
            @Override
            protected NodeResult doExecute(FlowExecutionContext context) throws Exception {
                attempts.incrementAndGet();
                throw new IllegalStateException("fail");
            }
        };
        assertThrows(Exception.class, () -> node.execute(new FlowExecutionContext()));
        assertEquals(3, attempts.get());
    }
}
